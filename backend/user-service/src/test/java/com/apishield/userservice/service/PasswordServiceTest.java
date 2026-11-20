package com.apishield.userservice.service;

import com.apishield.userservice.dto.request.ChangePasswordRequest;
import com.apishield.userservice.dto.request.ForgotPasswordRequest;
import com.apishield.userservice.dto.request.ResetPasswordRequest;
import com.apishield.userservice.entity.PasswordResetToken;
import com.apishield.userservice.entity.User;
import com.apishield.userservice.enums.AccountStatus;
import com.apishield.userservice.enums.Role;
import com.apishield.userservice.exception.InvalidCredentialsException;
import com.apishield.userservice.exception.InvalidTokenException;
import com.apishield.userservice.repository.PasswordResetTokenRepository;
import com.apishield.userservice.repository.UserRepository;
import com.apishield.userservice.service.impl.PasswordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PasswordServiceImpl passwordService;

    private User sampleUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(userId)
                .email("user@apishield.io")
                .passwordHash("currentHashedPassword")
                .role(Role.DEVELOPER)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build();
    }

    @Test
    @DisplayName("changePassword successfully hashes new password and revokes all refresh sessions")
    void changePassword_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("CurrentP@ss1", "currentHashedPassword")).thenReturn(true);
        when(passwordEncoder.matches("NewP@ssw0rd!", "currentHashedPassword")).thenReturn(false);
        when(passwordEncoder.encode("NewP@ssw0rd!")).thenReturn("newEncodedPassword");

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("CurrentP@ss1")
                .newPassword("NewP@ssw0rd!")
                .confirmNewPassword("NewP@ssw0rd!")
                .build();

        passwordService.changePassword(userId, request);

        assertThat(sampleUser.getPasswordHash()).isEqualTo("newEncodedPassword");
        verify(userRepository).save(sampleUser);
        verify(refreshTokenService).revokeAllUserSessions(eq(userId), eq("PASSWORD_CHANGE"));
    }

    @Test
    @DisplayName("changePassword throws InvalidCredentialsException when current password does not match")
    void changePassword_wrongCurrentPassword_throwsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongPassword", "currentHashedPassword")).thenReturn(false);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("WrongPassword")
                .newPassword("NewP@ssw0rd!")
                .confirmNewPassword("NewP@ssw0rd!")
                .build();

        assertThatThrownBy(() -> passwordService.changePassword(userId, request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Current password is incorrect");

        verify(refreshTokenService, never()).revokeAllUserSessions(any(), any());
    }

    @Test
    @DisplayName("changePassword throws IllegalArgumentException when new password matches current password")
    void changePassword_samePassword_throwsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("CurrentP@ss1", "currentHashedPassword")).thenReturn(true);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("CurrentP@ss1")
                .newPassword("CurrentP@ss1")
                .confirmNewPassword("CurrentP@ss1")
                .build();

        assertThatThrownBy(() -> passwordService.changePassword(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be the same");
    }

    @Test
    @DisplayName("changePassword throws IllegalArgumentException when confirmation does not match")
    void changePassword_mismatch_throwsException() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("CurrentP@ss1")
                .newPassword("NewP@ssw0rd!")
                .confirmNewPassword("DifferentP@ss!")
                .build();

        assertThatThrownBy(() -> passwordService.changePassword(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("do not match");
    }

    @Test
    @DisplayName("forgotPassword creates hashed single-use token and notifies user")
    void forgotPassword_success() {
        when(userRepository.findByEmail("user@apishield.io")).thenReturn(Optional.of(sampleUser));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(inv -> inv.getArgument(0));

        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("  USER@APISHIELD.IO  ")
                .build();

        passwordService.forgotPassword(request);

        verify(passwordResetTokenRepository).invalidateAllActiveTokensForUser(eq(userId), any(Instant.class));
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isEqualTo(sampleUser);
        assertThat(savedToken.getTokenHash()).hasSize(64); // SHA-256 hex
        assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());
        assertThat(savedToken.getUsedAt()).isNull();

        verify(notificationService).sendPasswordResetEmail(eq("user@apishield.io"), anyString());
    }

    @Test
    @DisplayName("forgotPassword with unknown email does not throw and prevents user enumeration")
    void forgotPassword_unknownEmail_doesNotThrow() {
        when(userRepository.findByEmail("unknown@apishield.io")).thenReturn(Optional.empty());

        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("unknown@apishield.io")
                .build();

        passwordService.forgotPassword(request);

        verify(passwordResetTokenRepository, never()).save(any());
        verify(notificationService, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    @DisplayName("resetPassword successfully verifies token, updates password, and revokes sessions")
    void resetPassword_success() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .tokenHash("some-hash")
                .expiresAt(Instant.now().plusSeconds(600))
                .usedAt(null)
                .build();

        when(passwordResetTokenRepository.findByTokenHashWithLock(anyString())).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("NewSecureP@ss1")).thenReturn("newHashedPassword");

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("raw-reset-token")
                .newPassword("NewSecureP@ss1")
                .confirmNewPassword("NewSecureP@ss1")
                .build();

        passwordService.resetPassword(request);

        assertThat(token.isUsed()).isTrue();
        assertThat(token.getUsedAt()).isNotNull();
        assertThat(sampleUser.getPasswordHash()).isEqualTo("newHashedPassword");

        verify(passwordResetTokenRepository).save(token);
        verify(userRepository).save(sampleUser);
        verify(refreshTokenService).revokeAllUserSessions(eq(userId), eq("PASSWORD_RESET"));
    }

    @Test
    @DisplayName("resetPassword throws InvalidTokenException when token is expired")
    void resetPassword_expiredToken_throwsException() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .tokenHash("some-hash")
                .expiresAt(Instant.now().minusSeconds(10))
                .usedAt(null)
                .build();

        when(passwordResetTokenRepository.findByTokenHashWithLock(anyString())).thenReturn(Optional.of(token));

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("raw-reset-token")
                .newPassword("NewSecureP@ss1")
                .confirmNewPassword("NewSecureP@ss1")
                .build();

        assertThatThrownBy(() -> passwordService.resetPassword(request))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("resetPassword throws InvalidTokenException when token has already been used")
    void resetPassword_reusedToken_throwsException() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .tokenHash("some-hash")
                .expiresAt(Instant.now().plusSeconds(600))
                .usedAt(Instant.now().minusSeconds(60))
                .build();

        when(passwordResetTokenRepository.findByTokenHashWithLock(anyString())).thenReturn(Optional.of(token));

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("raw-reset-token")
                .newPassword("NewSecureP@ss1")
                .confirmNewPassword("NewSecureP@ss1")
                .build();

        assertThatThrownBy(() -> passwordService.resetPassword(request))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("already been used");
    }
}
