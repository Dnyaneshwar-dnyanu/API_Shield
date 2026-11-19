package com.apishield.userservice.service.impl;

import com.apishield.userservice.dto.request.RegisterRequest;
import com.apishield.userservice.dto.response.UserResponse;
import com.apishield.userservice.entity.User;
import com.apishield.userservice.enums.AccountStatus;
import com.apishield.userservice.enums.Role;
import com.apishield.userservice.exception.DuplicateEmailException;
import com.apishield.userservice.exception.InvalidRoleException;
import com.apishield.userservice.repository.UserRepository;
import com.apishield.userservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // Validate requested role: public registration assigns DEVELOPER and prohibits self-assigning ADMIN or API_OWNER
        Role assignedRole = validateAndResolveRole(request.getRole());

        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Registration rejected: duplicate email '{}'", normalizedEmail);
            throw new DuplicateEmailException("A user with email '" + normalizedEmail + "' already exists.");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordHash)
                .role(assignedRole)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Successfully registered user with id: {} and email: {}", savedUser.getId(), savedUser.getEmail());

        return UserResponse.builder()
                .id(savedUser.getId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .accountStatus(savedUser.getAccountStatus())
                .emailVerified(savedUser.isEmailVerified())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    private Role validateAndResolveRole(String requestedRole) {
        if (requestedRole == null || requestedRole.trim().isEmpty()) {
            return Role.DEVELOPER;
        }

        String normalizedRole = requestedRole.trim().toUpperCase();

        if (Role.ADMIN.name().equals(normalizedRole) || Role.API_OWNER.name().equals(normalizedRole)) {
            throw new InvalidRoleException("Public registration does not allow self-assignment of role: " + requestedRole);
        }

        if (Role.DEVELOPER.name().equals(normalizedRole)) {
            return Role.DEVELOPER;
        }

        throw new InvalidRoleException("Invalid role specified: " + requestedRole + ". Allowed registration role is DEVELOPER.");
    }
}
