package com.apishield.userservice.service.impl;

import com.apishield.userservice.dto.response.RefreshTokenResult;
import com.apishield.userservice.dto.response.TokenRotationResult;
import com.apishield.userservice.dto.response.UserResponse;
import com.apishield.userservice.entity.RefreshToken;
import com.apishield.userservice.entity.User;
import com.apishield.userservice.enums.AccountStatus;
import com.apishield.userservice.exception.AccountInactiveException;
import com.apishield.userservice.exception.AccountSuspendedException;
import com.apishield.userservice.exception.InvalidRefreshTokenException;
import com.apishield.userservice.exception.TokenReuseException;
import com.apishield.userservice.repository.RefreshTokenRepository;
import com.apishield.userservice.security.JwtProperties;
import com.apishield.userservice.security.JwtService;
import com.apishield.userservice.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
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
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final int TOKEN_BYTE_LENGTH = 32; // 256 bits of cryptographic randomness
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final JwtService jwtService;

    @Override
    @Transactional
    public RefreshTokenResult createRefreshToken(User user) {
        String rawToken = generateSecureRandomToken();
        String tokenHash = hashToken(rawToken);
        UUID familyId = UUID.randomUUID();

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.getRefreshExpirationSeconds());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .familyId(familyId)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        log.info("Created new refresh token session for user ID: {}, family ID: {}", user.getId(), familyId);

        return RefreshTokenResult.builder()
                .rawRefreshToken(rawToken)
                .refreshToken(saved)
                .build();
    }

    @Override
    @Transactional(noRollbackFor = {TokenReuseException.class, InvalidRefreshTokenException.class})
    public TokenRotationResult rotateRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.trim().isEmpty()) {
            throw new InvalidRefreshTokenException("Refresh token is required.");
        }

        String tokenHash = hashToken(rawRefreshToken.trim());

        // Pessimistic write lock ensures atomic rotation and protects against race conditions
        RefreshToken storedToken = refreshTokenRepository.findByTokenHashWithLock(tokenHash)
                .orElseThrow(() -> {
                    log.warn("Refresh attempt with unknown refresh token hash");
                    return new InvalidRefreshTokenException("Invalid refresh token.");
                });

        UUID familyId = storedToken.getFamilyId();

        // 1. REUSE DETECTION: If this token was already revoked, terminate all tokens in the family
        if (storedToken.isRevoked()) {
            log.warn("SECURITY ALERT: Previously revoked refresh token presented! Family ID: {}, User ID: {}",
                    familyId, storedToken.getUser().getId());

            refreshTokenRepository.revokeAllByFamilyId(familyId, Instant.now(), "REUSE_DETECTED");

            throw new TokenReuseException("Refresh token reuse detected. All sessions in this family have been terminated.");
        }

        // 2. EXPIRATION CHECK
        if (storedToken.isExpired()) {
            log.info("Refresh token expired for user ID: {}, family ID: {}", storedToken.getUser().getId(), familyId);
            storedToken.setRevokedAt(Instant.now());
            storedToken.setRevokeReason("EXPIRED");
            refreshTokenRepository.save(storedToken);

            throw new InvalidRefreshTokenException("Refresh token has expired.");
        }

        // 3. ACCOUNT STATUS CHECK
        User user = storedToken.getUser();
        if (user.getAccountStatus() == AccountStatus.INACTIVE) {
            log.warn("Refresh blocked: account is INACTIVE for user ID: {}", user.getId());
            throw new AccountInactiveException("Account is inactive. Please contact support or verify your email.");
        }
        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            log.warn("Refresh blocked: account is SUSPENDED for user ID: {}", user.getId());
            throw new AccountSuspendedException("Account has been suspended. Please contact support.");
        }

        // 4. ATOMIC TOKEN ROTATION
        String newRawToken = generateSecureRandomToken();
        String newTokenHash = hashToken(newRawToken);
        Instant now = Instant.now();
        Instant newExpiresAt = now.plusSeconds(jwtProperties.getRefreshExpirationSeconds());

        // Save replacement token with the SAME family ID
        RefreshToken replacementToken = RefreshToken.builder()
                .user(user)
                .tokenHash(newTokenHash)
                .familyId(familyId)
                .createdAt(now)
                .expiresAt(newExpiresAt)
                .build();
        RefreshToken savedReplacement = refreshTokenRepository.save(replacementToken);

        // Invalidate old token and link to replacement
        storedToken.setRevokedAt(now);
        storedToken.setRevokeReason("ROTATED");
        storedToken.setReplacedByTokenId(savedReplacement.getId());
        refreshTokenRepository.save(storedToken);

        log.info("Successfully rotated refresh token for user ID: {}, family ID: {}", user.getId(), familyId);

        String newAccessToken = jwtService.generateToken(user);

        UserResponse userResponse = UserResponse.fromEntity(user);

        return TokenRotationResult.builder()
                .newAccessToken(newAccessToken)
                .newRawRefreshToken(newRawToken)
                .expiresIn(jwtService.getExpirationSeconds())
                .user(userResponse)
                .build();
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String rawRefreshToken, String reason) {
        if (rawRefreshToken == null || rawRefreshToken.trim().isEmpty()) {
            return;
        }

        String tokenHash = hashToken(rawRefreshToken.trim());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            if (!token.isRevoked()) {
                token.setRevokedAt(Instant.now());
                token.setRevokeReason(reason);
                refreshTokenRepository.save(token);
                log.info("Revoked refresh token for user ID: {}, reason: {}", token.getUser().getId(), reason);
            }
        });
    }

    @Override
    @Transactional
    public void revokeAllUserSessions(UUID userId, String reason) {
        int revokedCount = refreshTokenRepository.revokeAllByUserId(userId, Instant.now(), reason);
        log.info("Revoked {} active refresh token sessions for user ID: {}, reason: {}", revokedCount, userId, reason);
    }

    @Override
    public ResponseCookie createRefreshTokenCookie(String rawRefreshToken) {
        return ResponseCookie.from(jwtProperties.getRefreshCookieName(), rawRefreshToken)
                .httpOnly(true)
                .secure(jwtProperties.isRefreshCookieSecure())
                .path(jwtProperties.getRefreshCookiePath())
                .maxAge(jwtProperties.getRefreshExpirationSeconds())
                .sameSite(jwtProperties.getRefreshCookieSameSite())
                .build();
    }

    @Override
    public ResponseCookie createCleanRefreshTokenCookie() {
        return ResponseCookie.from(jwtProperties.getRefreshCookieName(), "")
                .httpOnly(true)
                .secure(jwtProperties.isRefreshCookieSecure())
                .path(jwtProperties.getRefreshCookiePath())
                .maxAge(0)
                .sameSite(jwtProperties.getRefreshCookieSameSite())
                .build();
    }

    private String generateSecureRandomToken() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
