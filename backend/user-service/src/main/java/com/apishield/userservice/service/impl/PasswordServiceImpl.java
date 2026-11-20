package com.apishield.userservice.service.impl;

import com.apishield.userservice.dto.request.ChangePasswordRequest;
import com.apishield.userservice.dto.request.ForgotPasswordRequest;
import com.apishield.userservice.dto.request.ResetPasswordRequest;
import com.apishield.userservice.entity.PasswordResetToken;
import com.apishield.userservice.entity.User;
import com.apishield.userservice.exception.InvalidCredentialsException;
import com.apishield.userservice.exception.InvalidTokenException;
import com.apishield.userservice.exception.ResourceNotFoundException;
import com.apishield.userservice.repository.PasswordResetTokenRepository;
import com.apishield.userservice.repository.UserRepository;
import com.apishield.userservice.service.NotificationService;
import com.apishield.userservice.service.PasswordService;
import com.apishield.userservice.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private static final int RESET_TOKEN_BYTE_LENGTH = 32; // 256 bits
    private static final long RESET_TOKEN_EXPIRATION_SECONDS = 900; // 15 minutes
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        log.info("Processing password change request for user ID: {}", userId);

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new IllegalArgumentException("New password and confirmation password do not match.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password cannot be the same as the current password.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all active refresh tokens for the user
        refreshTokenService.revokeAllUserSessions(userId, "PASSWORD_CHANGE");
        log.info("Successfully changed password and revoked all refresh sessions for user ID: {}", userId);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        log.info("Processing forgot-password request for email");

        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            // Invalidate any previously outstanding reset tokens for this user
            passwordResetTokenRepository.invalidateAllActiveTokensForUser(user.getId(), Instant.now());

            String rawToken = generateSecureToken();
            String tokenHash = hashToken(rawToken);

            Instant expiresAt = Instant.now().plusSeconds(RESET_TOKEN_EXPIRATION_SECONDS);

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(tokenHash)
                    .expiresAt(expiresAt)
                    .build();

            passwordResetTokenRepository.save(resetToken);

            notificationService.sendPasswordResetEmail(normalizedEmail, rawToken);
            log.info("Created password reset token for user ID: {}", user.getId());
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Processing password reset with token");

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new IllegalArgumentException("New password and confirmation password do not match.");
        }

        String rawToken = request.getToken().trim();
        String tokenHash = hashToken(rawToken);

        // Pessimistic write lock prevents simultaneous race conditions during token consumption
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHashWithLock(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired password reset token."));

        if (resetToken.isUsed()) {
            log.warn("Attempted reuse of consumed password reset token ID: {}", resetToken.getId());
            throw new InvalidTokenException("Password reset token has already been used.");
        }

        if (resetToken.isExpired()) {
            log.warn("Attempted use of expired password reset token ID: {}", resetToken.getId());
            throw new InvalidTokenException("Password reset token has expired.");
        }

        // Mark token as consumed
        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);

        // Update user password
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidate all active refresh tokens
        refreshTokenService.revokeAllUserSessions(user.getId(), "PASSWORD_RESET");
        log.info("Successfully reset password and revoked all refresh sessions for user ID: {}", user.getId());
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[RESET_TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
