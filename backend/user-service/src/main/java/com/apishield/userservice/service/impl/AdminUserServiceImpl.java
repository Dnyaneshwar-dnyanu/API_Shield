package com.apishield.userservice.service.impl;

import com.apishield.userservice.dto.request.AdminUpdateUserRequest;
import com.apishield.userservice.dto.request.SuspendUserRequest;
import com.apishield.userservice.dto.request.UpdateUserRoleRequest;
import com.apishield.userservice.dto.response.PageResponse;
import com.apishield.userservice.dto.response.UserResponse;
import com.apishield.userservice.entity.User;
import com.apishield.userservice.enums.AccountStatus;
import com.apishield.userservice.enums.Role;
import com.apishield.userservice.exception.ResourceNotFoundException;
import com.apishield.userservice.repository.UserRepository;
import com.apishield.userservice.service.AccountService;
import com.apishield.userservice.service.AdminUserService;
import com.apishield.userservice.service.AuditLogService;
import com.apishield.userservice.service.RefreshTokenService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final AccountService accountService;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(String search, Role role, AccountStatus status, Pageable pageable) {
        log.debug("Listing users with search='{}', role={}, status={}, pageable={}", search, role, status, pageable);

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate firstNameMatch = cb.like(cb.lower(root.get("firstName")), pattern);
                Predicate lastNameMatch = cb.like(cb.lower(root.get("lastName")), pattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), pattern);
                predicates.add(cb.or(firstNameMatch, lastNameMatch, emailMatch));
            }

            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("accountStatus"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> page = userRepository.findAll(spec, pageable);
        return PageResponse.of(page.map(UserResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserResponse updateUserDetails(UUID userId, AdminUpdateUserRequest request, UUID adminId) {
        log.info("Admin {} updating details for user {}", adminId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        User saved = userRepository.save(user);

        auditLogService.logAction(adminId, userId, "UPDATE_DETAILS", "Updated names to " + user.getFirstName() + " " + user.getLastName());
        return UserResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public UserResponse updateUserRole(UUID userId, UpdateUserRoleRequest request, UUID adminId) {
        log.info("Admin {} updating role for user {} to {}", adminId, userId, request.getRole());

        if (userId.equals(adminId)) {
            throw new IllegalArgumentException("Administrators cannot change their own role.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Prevent demoting the last active administrator
        if (user.getRole() == Role.ADMIN && request.getRole() != Role.ADMIN) {
            long activeAdminCount = userRepository.countByRoleAndAccountStatus(Role.ADMIN, AccountStatus.ACTIVE);
            if (activeAdminCount <= 1) {
                throw new IllegalStateException("Cannot demote the last active administrator.");
            }
        }

        Role oldRole = user.getRole();
        user.setRole(request.getRole());
        User saved = userRepository.save(user);

        // Revoke active sessions for target user so next token refresh / authentication reflects new role
        refreshTokenService.revokeAllUserSessions(userId, "ADMIN_ROLE_CHANGE");
        auditLogService.logAction(adminId, userId, "ROLE_CHANGE", "Changed role from " + oldRole + " to " + request.getRole());

        return UserResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public UserResponse activateUser(UUID userId, UUID adminId) {
        log.info("Admin {} activating user {}", adminId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setEmailVerified(true);
        User saved = userRepository.save(user);

        auditLogService.logAction(adminId, userId, "ACTIVATE_USER", "Administratively activated");
        return UserResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public UserResponse deactivateUser(UUID userId, UUID adminId) {
        log.info("Admin {} deactivating user {}", adminId, userId);

        if (userId.equals(adminId)) {
            throw new IllegalArgumentException("Administrators cannot deactivate their own account.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (user.getRole() == Role.ADMIN) {
            long activeAdminCount = userRepository.countByRoleAndAccountStatus(Role.ADMIN, AccountStatus.ACTIVE);
            if (activeAdminCount <= 1) {
                throw new IllegalStateException("Cannot deactivate the last active administrator.");
            }
        }

        user.setAccountStatus(AccountStatus.INACTIVE);
        User saved = userRepository.save(user);

        refreshTokenService.revokeAllUserSessions(userId, "ADMIN_DEACTIVATE");
        auditLogService.logAction(adminId, userId, "DEACTIVATE_USER", "Administratively deactivated");

        return UserResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public UserResponse suspendUser(UUID userId, SuspendUserRequest request, UUID adminId) {
        log.info("Admin {} suspending user {}", adminId, userId);

        if (userId.equals(adminId)) {
            throw new IllegalArgumentException("Administrators cannot suspend their own account.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (user.getRole() == Role.ADMIN) {
            long activeAdminCount = userRepository.countByRoleAndAccountStatus(Role.ADMIN, AccountStatus.ACTIVE);
            if (activeAdminCount <= 1) {
                throw new IllegalStateException("Cannot suspend the last active administrator.");
            }
        }

        accountService.suspendAccount(userId, request != null ? request.getReason() : null, adminId);
        auditLogService.logAction(adminId, userId, "SUSPEND_USER", request != null ? request.getReason() : "Administratively suspended");

        User updated = userRepository.findById(userId).orElseThrow();
        return UserResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public UserResponse reactivateUser(UUID userId, UUID adminId) {
        log.info("Admin {} reactivating user {}", adminId, userId);

        accountService.reactivateAccount(userId, adminId);
        auditLogService.logAction(adminId, userId, "REACTIVATE_USER", "Administratively reactivated");

        User updated = userRepository.findById(userId).orElseThrow();
        return UserResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteUser(UUID userId, UUID adminId) {
        log.info("Admin {} soft-deleting user {}", adminId, userId);

        if (userId.equals(adminId)) {
            throw new IllegalArgumentException("Administrators cannot delete their own account.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (user.getRole() == Role.ADMIN) {
            long activeAdminCount = userRepository.countByRoleAndAccountStatus(Role.ADMIN, AccountStatus.ACTIVE);
            if (activeAdminCount <= 1) {
                throw new IllegalStateException("Cannot delete the last active administrator.");
            }
        }

        // Soft-delete to preserve audit logs and referential integrity
        user.setAccountStatus(AccountStatus.INACTIVE);
        user.setSuspensionReason("DELETED_BY_ADMIN");
        userRepository.save(user);

        refreshTokenService.revokeAllUserSessions(userId, "ADMIN_DELETE");
        auditLogService.logAction(adminId, userId, "DELETE_USER", "Account soft-deleted by administrator");
    }
}
