package com.apishield.userservice.service;

import com.apishield.userservice.dto.response.RefreshTokenResult;
import com.apishield.userservice.dto.response.TokenRotationResult;
import com.apishield.userservice.entity.RefreshToken;
import com.apishield.userservice.entity.User;
import com.apishield.userservice.enums.AccountStatus;
import com.apishield.userservice.enums.Role;
import com.apishield.userservice.exception.AccountInactiveException;
import com.apishield.userservice.exception.AccountSuspendedException;
import com.apishield.userservice.exception.InvalidRefreshTokenException;
import com.apishield.userservice.exception.TokenReuseException;
import com.apishield.userservice.repository.RefreshTokenRepository;
import com.apishield.userservice.security.JwtProperties;
import com.apishield.userservice.security.JwtService;
import com.apishield.userservice.service.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    private User activeUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activeUser = User.builder()
                .id(userId)
                .email("user@apishield.io")
                .role(Role.DEVELOPER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        lenient().when(jwtProperties.getRefreshExpirationSeconds()).thenReturn(604800L);
        lenient().when(jwtProperties.getRefreshCookieName()).thenReturn("refresh_token");
        lenient().when(jwtProperties.getRefreshCookiePath()).thenReturn("/api/v1/auth");
        lenient().when(jwtProperties.getRefreshCookieSameSite()).thenReturn("Lax");
        lenient().when(jwtProperties.isRefreshCookieSecure()).thenReturn(false);
    }

    @Test
    @DisplayName("Create refresh token generates secure random token, stores SHA-256 hash, and sets family ID")
    void createRefreshToken_success() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> {
            RefreshToken token = inv.getArgument(0);
            if (token.getId() == null) {
                token.setId(UUID.randomUUID());
            }
            return token;
        });

        RefreshTokenResult result = refreshTokenService.createRefreshToken(activeUser);

        assertThat(result).isNotNull();
        assertThat(result.getRawRefreshToken()).isNotBlank();
        assertThat(result.getRefreshToken()).isNotNull();

        // Verify entity details
        RefreshToken stored = result.getRefreshToken();
        assertThat(stored.getUser()).isEqualTo(activeUser);
        assertThat(stored.getFamilyId()).isNotNull();
        assertThat(stored.getTokenHash()).hasSize(64); // SHA-256 hex is 64 characters
        assertThat(stored.getTokenHash()).isNotEqualTo(result.getRawRefreshToken()); // NEVER plain text!
        assertThat(stored.getExpiresAt()).isAfter(Instant.now());
        assertThat(stored.getRevokedAt()).isNull();
    }

    @Test
    @DisplayName("Rotate refresh token successfully rotates token and returns new tokens")
    void rotateRefreshToken_success() {
        UUID familyId = UUID.randomUUID();
        RefreshToken existingToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(activeUser)
                .tokenHash("some-hash")
                .familyId(familyId)
                .createdAt(Instant.now().minusSeconds(100))
                .expiresAt(Instant.now().plusSeconds(1000))
                .revokedAt(null)
                .build();

        when(refreshTokenRepository.findByTokenHashWithLock(anyString())).thenReturn(Optional.of(existingToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> {
            RefreshToken token = inv.getArgument(0);
            if (token.getId() == null) {
                token.setId(UUID.randomUUID());
            }
            return token;
        });
        when(jwtService.generateToken(activeUser)).thenReturn("new.access.token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        TokenRotationResult result = refreshTokenService.rotateRefreshToken("raw-presented-token");

        assertThat(result).isNotNull();
        assertThat(result.getNewAccessToken()).isEqualTo("new.access.token");
        assertThat(result.getNewRawRefreshToken()).isNotBlank();
        assertThat(result.getUser().getId()).isEqualTo(userId);

        // Verify old token was revoked with ROTATED reason
        assertThat(existingToken.getRevokedAt()).isNotNull();
        assertThat(existingToken.getRevokeReason()).isEqualTo("ROTATED");
        assertThat(existingToken.getReplacedByTokenId()).isNotNull();

        // Verify replacement token was saved in the same family
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(captor.capture());
        RefreshToken replacement = captor.getAllValues().get(0);
        assertThat(replacement.getFamilyId()).isEqualTo(familyId);
    }

    @Test
    @DisplayName("Token reuse detection revokes all tokens in the family and throws TokenReuseException")
    void rotateRefreshToken_reuseDetection_revokesFamily() {
        UUID familyId = UUID.randomUUID();
        RefreshToken previouslyRevokedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(activeUser)
                .tokenHash("reused-hash")
                .familyId(familyId)
                .createdAt(Instant.now().minusSeconds(200))
                .expiresAt(Instant.now().plusSeconds(1000))
                .revokedAt(Instant.now().minusSeconds(100))
                .revokeReason("ROTATED")
                .build();

        when(refreshTokenRepository.findByTokenHashWithLock(anyString())).thenReturn(Optional.of(previouslyRevokedToken));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("reused-token"))
                .isInstanceOf(TokenReuseException.class)
                .hasMessageContaining("reuse detected");

        verify(refreshTokenRepository).revokeAllByFamilyId(eq(familyId), any(Instant.class), eq("REUSE_DETECTED"));
    }

    @Test
    @DisplayName("Expired refresh token is marked revoked with EXPIRED reason and throws InvalidRefreshTokenException")
    void rotateRefreshToken_expired_throwsException() {
        RefreshToken expiredToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(activeUser)
                .familyId(UUID.randomUUID())
                .expiresAt(Instant.now().minusSeconds(60))
                .revokedAt(null)
                .build();

        when(refreshTokenRepository.findByTokenHashWithLock(anyString())).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("expired-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("expired");

        assertThat(expiredToken.getRevokedAt()).isNotNull();
        assertThat(expiredToken.getRevokeReason()).isEqualTo("EXPIRED");
        verify(refreshTokenRepository).save(expiredToken);
    }

    @Test
    @DisplayName("Unknown refresh token throws InvalidRefreshTokenException")
    void rotateRefreshToken_unknown_throwsException() {
        when(refreshTokenRepository.findByTokenHashWithLock(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("unknown-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    @DisplayName("Refresh blocked when account is INACTIVE")
    void rotateRefreshToken_inactiveAccount_throwsException() {
        activeUser.setAccountStatus(AccountStatus.INACTIVE);
        RefreshToken validToken = RefreshToken.builder()
                .user(activeUser)
                .expiresAt(Instant.now().plusSeconds(1000))
                .revokedAt(null)
                .build();

        when(refreshTokenRepository.findByTokenHashWithLock(anyString())).thenReturn(Optional.of(validToken));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("token"))
                .isInstanceOf(AccountInactiveException.class);
    }

    @Test
    @DisplayName("Refresh blocked when account is SUSPENDED")
    void rotateRefreshToken_suspendedAccount_throwsException() {
        activeUser.setAccountStatus(AccountStatus.SUSPENDED);
        RefreshToken validToken = RefreshToken.builder()
                .user(activeUser)
                .expiresAt(Instant.now().plusSeconds(1000))
                .revokedAt(null)
                .build();

        when(refreshTokenRepository.findByTokenHashWithLock(anyString())).thenReturn(Optional.of(validToken));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken("token"))
                .isInstanceOf(AccountSuspendedException.class);
    }

    @Test
    @DisplayName("Revoke refresh token revokes specific active session")
    void revokeRefreshToken_success() {
        RefreshToken activeToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(activeUser)
                .revokedAt(null)
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(activeToken));

        refreshTokenService.revokeRefreshToken("raw-token", "LOGOUT");

        assertThat(activeToken.getRevokedAt()).isNotNull();
        assertThat(activeToken.getRevokeReason()).isEqualTo("LOGOUT");
        verify(refreshTokenRepository).save(activeToken);
    }

    @Test
    @DisplayName("Revoke all user sessions revokes all active tokens for user")
    void revokeAllUserSessions_success() {
        refreshTokenService.revokeAllUserSessions(userId, "LOGOUT_ALL");

        verify(refreshTokenRepository).revokeAllByUserId(eq(userId), any(Instant.class), eq("LOGOUT_ALL"));
    }

    @Test
    @DisplayName("Cookie creation sets HttpOnly, Path, and MaxAge accurately")
    void createRefreshTokenCookie_properties() {
        ResponseCookie cookie = refreshTokenService.createRefreshTokenCookie("raw-token");

        assertThat(cookie.getName()).isEqualTo("refresh_token");
        assertThat(cookie.getValue()).isEqualTo("raw-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(604800L);
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
    }

    @Test
    @DisplayName("Clean cookie creation sets MaxAge to 0")
    void createCleanRefreshTokenCookie_clearsCookie() {
        ResponseCookie cookie = refreshTokenService.createCleanRefreshTokenCookie();

        assertThat(cookie.getName()).isEqualTo("refresh_token");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(0L);
    }
}
