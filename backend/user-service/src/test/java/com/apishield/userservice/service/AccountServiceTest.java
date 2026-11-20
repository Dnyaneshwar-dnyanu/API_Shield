package com.apishield.userservice.service;

import com.apishield.userservice.dto.request.DeactivateAccountRequest;
import com.apishield.userservice.dto.request.ResendVerificationRequest;
import com.apishield.userservice.dto.request.VerifyEmailRequest;
import com.apishield.userservice.entity.EmailVerificationToken;
import com.apishield.userservice.entity.User;
import com.apishield.userservice.enums.AccountStatus;
import com.apishield.userservice.enums.Role;
import com.apishield.userservice.exception.InvalidCredentialsException;
import com.apishield.userservice.exception.InvalidTokenException;
import com.apishield.userservice.repository.EmailVerificationTokenRepository;
import com.apishield.userservice.repository.UserRepository;
import com.apishield.userservice.service.impl.AccountServiceImpl;
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
class AccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AccountServiceImpl accountService;

    private User sampleUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(userId)
                .email("user@apishield.io")
                .passwordHash("hashedPass")
                .role(Role.DEVELOPER)
                .accountStatus(AccountStatus.INACTIVE)
                .emailVerified(false)
                .build();
    }

    @Test
    @DisplayName("verifyEmail successfully verifies email and activates INACTIVE account")
    void verifyEmail_activatesInactiveAccount() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .tokenHash("hash")
                .expiresAt(Instant.now().plusSeconds(3600))
                .usedAt(null)
                .build();

        when(emailVerificationTokenRepository.findByTokenHashWithLock(anyString())).thenReturn(Optional.of(token));

        accountService.verifyEmail(new VerifyEmailRequest("raw-verification-token"));

        assertThat(sampleUser.isEmailVerified()).isTrue();
        assertThat(sampleUser.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(token.isUsed()).isTrue();

        verify(emailVerificationTokenRepository).save(token);
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("verifyEmail does NOT automatically activate an administratively SUSPENDED account")
    void verifyEmail_doesNotActivateSuspendedAccount() {
        sampleUser.setAccountStatus(AccountStatus.SUSPENDED);
        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .tokenHash("hash")
                .expiresAt(Instant.now().plusSeconds(3600))
                .usedAt(null)
                .build();

        when(emailVerificationTokenRepository.findByTokenHashWithLock(anyString())).thenReturn(Optional.of(token));

        accountService.verifyEmail(new VerifyEmailRequest("raw-token"));

        assertThat(sampleUser.isEmailVerified()).isTrue();
        assertThat(sampleUser.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED); // Must remain SUSPENDED!
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("verifyEmail throws InvalidTokenException for already used token")
    void verifyEmail_reusedToken_throwsException() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .tokenHash("hash")
                .expiresAt(Instant.now().plusSeconds(3600))
                .usedAt(Instant.now().minusSeconds(100))
                .build();

        when(emailVerificationTokenRepository.findByTokenHashWithLock(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> accountService.verifyEmail(new VerifyEmailRequest("raw-token")))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    @DisplayName("verifyEmail throws InvalidTokenException for expired token")
    void verifyEmail_expiredToken_throwsException() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .tokenHash("hash")
                .expiresAt(Instant.now().minusSeconds(60))
                .usedAt(null)
                .build();

        when(emailVerificationTokenRepository.findByTokenHashWithLock(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> accountService.verifyEmail(new VerifyEmailRequest("raw-token")))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("deactivateAccount successfully sets INACTIVE and revokes all refresh sessions")
    void deactivateAccount_success() {
        sampleUser.setAccountStatus(AccountStatus.ACTIVE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("ValidPassword123!", "hashedPass")).thenReturn(true);

        accountService.deactivateAccount(userId, new DeactivateAccountRequest("ValidPassword123!"));

        assertThat(sampleUser.getAccountStatus()).isEqualTo(AccountStatus.INACTIVE);
        verify(userRepository).save(sampleUser);
        verify(refreshTokenService).revokeAllUserSessions(eq(userId), eq("ACCOUNT_DEACTIVATED"));
    }

    @Test
    @DisplayName("deactivateAccount throws InvalidCredentialsException on incorrect password")
    void deactivateAccount_wrongPassword_throwsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongPassword", "hashedPass")).thenReturn(false);

        assertThatThrownBy(() -> accountService.deactivateAccount(userId, new DeactivateAccountRequest("WrongPassword")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Incorrect password");

        verify(refreshTokenService, never()).revokeAllUserSessions(any(), any());
    }

    @Test
    @DisplayName("suspendAccount sets SUSPENDED status, audit metadata, and revokes sessions")
    void suspendAccount_success() {
        UUID adminId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        accountService.suspendAccount(userId, "Violation of platform terms", adminId);

        assertThat(sampleUser.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(sampleUser.getSuspensionReason()).isEqualTo("Violation of platform terms");
        assertThat(sampleUser.getSuspendedBy()).isEqualTo(adminId);
        assertThat(sampleUser.getSuspendedAt()).isNotNull();

        verify(userRepository).save(sampleUser);
        verify(refreshTokenService).revokeAllUserSessions(eq(userId), eq("ADMIN_SUSPENSION"));
    }

    @Test
    @DisplayName("reactivateAccount restores ACTIVE status and clears suspension metadata")
    void reactivateAccount_success() {
        sampleUser.setAccountStatus(AccountStatus.SUSPENDED);
        sampleUser.setSuspensionReason("Violation");
        sampleUser.setSuspendedBy(UUID.randomUUID());
        sampleUser.setSuspendedAt(Instant.now());

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        accountService.reactivateAccount(userId, UUID.randomUUID());

        assertThat(sampleUser.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(sampleUser.getSuspensionReason()).isNull();
        assertThat(sampleUser.getSuspendedBy()).isNull();
        assertThat(sampleUser.getSuspendedAt()).isNull();

        verify(userRepository).save(sampleUser);
    }
}
