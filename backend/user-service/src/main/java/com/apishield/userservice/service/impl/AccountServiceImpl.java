package com.apishield.userservice.service.impl;

import com.apishield.userservice.dto.request.DeactivateAccountRequest;
import com.apishield.userservice.dto.request.ResendVerificationRequest;
import com.apishield.userservice.dto.request.VerifyEmailRequest;
import com.apishield.userservice.entity.EmailVerificationToken;
import com.apishield.userservice.entity.User;
import com.apishield.userservice.enums.AccountStatus;
import com.apishield.userservice.exception.InvalidCredentialsException;
import com.apishield.userservice.exception.InvalidTokenException;
import com.apishield.userservice.exception.ResourceNotFoundException;
import com.apishield.userservice.repository.EmailVerificationTokenRepository;
import com.apishield.userservice.repository.UserRepository;
import com.apishield.userservice.service.AccountService;
import com.apishield.userservice.service.NotificationService;
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
public class AccountServiceImpl implements AccountService {

    private static final int VERIFICATION_TOKEN_BYTE_LENGTH = 32; // 256 bits
    private static final long VERIFICATION_TOKEN_EXPIRATION_SECONDS = 86400; // 24 hours
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        log.info("Processing email verification request");

        String rawToken = request.getToken().trim();
        String tokenHash = hashToken(rawToken);

        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByTokenHashWithLock(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired email verification token."));

        if (verificationToken.isUsed()) {
            log.warn("Attempted reuse of email verification token ID: {}", verificationToken.getId());
            throw new InvalidTokenException("Email verification token has already been used.");
        }

        if (verificationToken.isExpired()) {
            log.warn("Attempted use of expired email verification token ID: {}", verificationToken.getId());
            throw new InvalidTokenException("Email verification token has expired.");
        }

        verificationToken.setUsedAt(Instant.now());
        emailVerificationTokenRepository.save(verificationToken);

        User user = verificationToken.getUser();
        user.setEmailVerified(true);

        // If account is INACTIVE, email verification activates it.
        // SECURITY REQUIREMENT: Do NOT automatically activate accounts that are SUSPENDED by an administrator!
        if (user.getAccountStatus() == AccountStatus.INACTIVE) {
            user.setAccountStatus(AccountStatus.ACTIVE);
            log.info("Account activated upon email verification for user ID: {}", user.getId());
        }

        userRepository.save(user);
        log.info("Successfully verified email for user ID: {}", user.getId());
    }

    @Override
    @Transactional
    public void resendVerificationEmail(ResendVerificationRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        log.info("Processing resend verification email request");

        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            if (user.isEmailVerified()) {
                log.info("Skipping verification email: User ID: {} is already verified", user.getId());
                return;
            }

            emailVerificationTokenRepository.invalidateAllActiveTokensForUser(user.getId(), Instant.now());

            String rawToken = generateSecureToken();
            String tokenHash = hashToken(rawToken);
            Instant expiresAt = Instant.now().plusSeconds(VERIFICATION_TOKEN_EXPIRATION_SECONDS);

            EmailVerificationToken token = EmailVerificationToken.builder()
                    .user(user)
                    .tokenHash(tokenHash)
                    .expiresAt(expiresAt)
                    .build();

            emailVerificationTokenRepository.save(token);
            notificationService.sendEmailVerification(normalizedEmail, rawToken);
            log.info("Issued new email verification token for user ID: {}", user.getId());
        });
    }

    @Override
    @Transactional
    public void deactivateAccount(UUID userId, DeactivateAccountRequest request) {
        log.info("Processing self-service account deactivation for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Incorrect password. Account deactivation cancelled.");
        }

        user.setAccountStatus(AccountStatus.INACTIVE);
        userRepository.save(user);

        // Terminate all active refresh sessions
        refreshTokenService.revokeAllUserSessions(userId, "ACCOUNT_DEACTIVATED");
        log.info("Successfully deactivated account and terminated all refresh sessions for user ID: {}", userId);
    }

    @Override
    @Transactional
    public void suspendAccount(UUID userId, String reason, UUID adminId) {
        log.info("Processing administrative suspension for user ID: {} by admin: {}", userId, adminId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        user.setAccountStatus(AccountStatus.SUSPENDED);
        user.setSuspensionReason(reason != null ? reason.trim() : "Administratively suspended");
        user.setSuspendedAt(Instant.now());
        user.setSuspendedBy(adminId);
        userRepository.save(user);

        // Revoke all refresh tokens immediately
        refreshTokenService.revokeAllUserSessions(userId, "ADMIN_SUSPENSION");
        log.info("Account suspended for user ID: {} with reason: {}", userId, user.getSuspensionReason());
    }

    @Override
    @Transactional
    public void reactivateAccount(UUID userId, UUID adminId) {
        log.info("Processing administrative reactivation for user ID: {} by admin: {}", userId, adminId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setSuspensionReason(null);
        user.setSuspendedAt(null);
        user.setSuspendedBy(null);
        userRepository.save(user);

        log.info("Account successfully reactivated for user ID: {}", userId);
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[VERIFICATION_TOKEN_BYTE_LENGTH];
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
