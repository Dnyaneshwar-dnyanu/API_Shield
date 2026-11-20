package com.apishield.userservice.service;

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
import com.apishield.userservice.service.impl.AdminUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private User sampleUser;
    private UUID targetUserId;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        targetUserId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(targetUserId)
                .firstName("John")
                .lastName("Doe")
                .email("john@apishield.io")
                .role(Role.DEVELOPER)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build();
    }

    @Test
    @DisplayName("listUsers returns paged user response")
    void listUsers_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(Collections.singletonList(sampleUser), pageable, 1);
        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        PageResponse<UserResponse> response = adminUserService.listUsers("john", Role.DEVELOPER, AccountStatus.ACTIVE, pageable);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getEmail()).isEqualTo("john@apishield.io");
    }

    @Test
    @DisplayName("updateUserRole changes role, revokes sessions, and records audit log")
    void updateUserRole_success() {
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = adminUserService.updateUserRole(
                targetUserId,
                new UpdateUserRoleRequest(Role.API_OWNER),
                adminId
        );

        assertThat(response.getRole()).isEqualTo(Role.API_OWNER);
        assertThat(sampleUser.getRole()).isEqualTo(Role.API_OWNER);
        verify(refreshTokenService).revokeAllUserSessions(eq(targetUserId), eq("ADMIN_ROLE_CHANGE"));
        verify(auditLogService).logAction(eq(adminId), eq(targetUserId), eq("ROLE_CHANGE"), anyString());
    }

    @Test
    @DisplayName("updateUserRole rejects administrator attempting to change own role")
    void updateUserRole_selfPromotionOrDemotion_throwsException() {
        assertThatThrownBy(() -> adminUserService.updateUserRole(
                adminId,
                new UpdateUserRoleRequest(Role.DEVELOPER),
                adminId
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot change their own role");
    }

    @Test
    @DisplayName("updateUserRole rejects demoting the last active administrator")
    void updateUserRole_lastActiveAdmin_throwsException() {
        sampleUser.setRole(Role.ADMIN);
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.countByRoleAndAccountStatus(Role.ADMIN, AccountStatus.ACTIVE)).thenReturn(1L);

        assertThatThrownBy(() -> adminUserService.updateUserRole(
                targetUserId,
                new UpdateUserRoleRequest(Role.DEVELOPER),
                adminId
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("last active administrator");
    }

    @Test
    @DisplayName("suspendUser suspends target user, revokes sessions, and records audit log")
    void suspendUser_success() {
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(sampleUser));

        adminUserService.suspendUser(targetUserId, new SuspendUserRequest("Spamming"), adminId);

        verify(accountService).suspendAccount(eq(targetUserId), eq("Spamming"), eq(adminId));
        verify(auditLogService).logAction(eq(adminId), eq(targetUserId), eq("SUSPEND_USER"), eq("Spamming"));
    }

    @Test
    @DisplayName("suspendUser rejects administrator attempting to suspend own account")
    void suspendUser_selfSuspension_throwsException() {
        assertThatThrownBy(() -> adminUserService.suspendUser(
                adminId,
                new SuspendUserRequest("Reason"),
                adminId
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot suspend their own account");
    }
}
