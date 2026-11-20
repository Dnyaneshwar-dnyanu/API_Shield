package com.apishield.userservice.service.impl;

import com.apishield.userservice.dto.request.LoginRequest;
import com.apishield.userservice.dto.request.RegisterRequest;
import com.apishield.userservice.dto.response.AuthResult;
import com.apishield.userservice.dto.response.LoginResponse;
import com.apishield.userservice.dto.response.RefreshTokenResult;
import com.apishield.userservice.dto.response.UserResponse;
import com.apishield.userservice.entity.User;
import com.apishield.userservice.enums.AccountStatus;
import com.apishield.userservice.enums.Role;
import com.apishield.userservice.exception.AccountInactiveException;
import com.apishield.userservice.exception.AccountSuspendedException;
import com.apishield.userservice.exception.DuplicateEmailException;
import com.apishield.userservice.exception.InvalidCredentialsException;
import com.apishield.userservice.exception.InvalidRoleException;
import com.apishield.userservice.exception.ResourceNotFoundException;
import com.apishield.userservice.repository.UserRepository;
import com.apishield.userservice.security.JwtService;
import com.apishield.userservice.service.AuthService;
import com.apishield.userservice.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

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

        return mapToUserResponse(savedUser);
    }

    @Override
    @Transactional
    public AuthResult login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Login failed: account not found for normalized email '{}'", normalizedEmail);
                    return new InvalidCredentialsException("Invalid email or password.");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: incorrect password for email '{}'", normalizedEmail);
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        if (user.getAccountStatus() == AccountStatus.INACTIVE) {
            log.warn("Login blocked: user {} account is INACTIVE", user.getId());
            throw new AccountInactiveException("Account is inactive. Please verify your email or contact support.");
        }

        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            log.warn("Login blocked: user {} account is SUSPENDED", user.getId());
            throw new AccountSuspendedException("Account has been suspended. Please contact support.");
        }

        String accessToken = jwtService.generateToken(user);
        RefreshTokenResult refreshResult = refreshTokenService.createRefreshToken(user);

        log.info("Successfully authenticated user with id: {}, issued access token and refresh session", user.getId());

        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationSeconds())
                .user(mapToUserResponse(user))
                .build();

        return AuthResult.builder()
                .loginResponse(loginResponse)
                .rawRefreshToken(refreshResult.getRawRefreshToken())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return mapToUserResponse(user);
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

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.fromEntity(user);
    }
}
