package com.apishield.userservice.service;

import com.apishield.userservice.dto.request.LoginRequest;
import com.apishield.userservice.dto.request.RegisterRequest;
import com.apishield.userservice.dto.response.AuthResult;
import com.apishield.userservice.dto.response.RefreshTokenResult;
import com.apishield.userservice.dto.response.UserResponse;
import com.apishield.userservice.entity.RefreshToken;
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
import com.apishield.userservice.service.impl.AuthServiceImpl;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private User activeUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        validRegisterRequest = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("SecurePass123!")
                .role(null)
                .build();

        validLoginRequest = LoginRequest.builder()
                .email("john.doe@example.com")
                .password("SecurePass123!")
                .build();

        activeUser = User.builder()
                .id(userId)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .passwordHash("hashed_bcrypt_password")
                .role(Role.DEVELOPER)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // ================= REGISTER TESTS =================

    @Test
    @DisplayName("Successfully register developer when role is not specified")
    void register_success_defaultRole() {
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("hashed_bcrypt_password");

        UUID generatedId = UUID.randomUUID();
        Instant now = Instant.now();
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(generatedId);
            u.setCreatedAt(now);
            return u;
        });

        UserResponse response = authService.register(validRegisterRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(generatedId);
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(response.getRole()).isEqualTo(Role.DEVELOPER);
        assertThat(response.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(response.isEmailVerified()).isFalse();
        assertThat(response.getCreatedAt()).isEqualTo(now);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("hashed_bcrypt_password");
        assertThat(saved.getRole()).isEqualTo(Role.DEVELOPER);
    }

    @Test
    @DisplayName("Successfully register developer when DEVELOPER role is explicitly requested")
    void register_success_explicitDeveloperRole() {
        validRegisterRequest.setRole("DEVELOPER");
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = authService.register(validRegisterRequest);

        assertThat(response.getRole()).isEqualTo(Role.DEVELOPER);
    }

    @Test
    @DisplayName("Email is normalized by trimming whitespace and converting to lowercase")
    void register_normalizesEmail() {
        validRegisterRequest.setEmail("  John.DOE@ExAMple.COM  ");
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = authService.register(validRegisterRequest);

        assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
        verify(userRepository).existsByEmail("john.doe@example.com");
    }

    @Test
    @DisplayName("Throws DuplicateEmailException when email already exists")
    void register_duplicateEmail_throwsException() {
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Throws InvalidRoleException when client attempts to self-assign ADMIN role")
    void register_adminRole_throwsInvalidRoleException() {
        validRegisterRequest.setRole("ADMIN");

        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(InvalidRoleException.class)
                .hasMessageContaining("does not allow self-assignment");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Throws InvalidRoleException when client attempts to self-assign API_OWNER role")
    void register_apiOwnerRole_throwsInvalidRoleException() {
        validRegisterRequest.setRole("API_OWNER");

        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(InvalidRoleException.class)
                .hasMessageContaining("does not allow self-assignment");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Throws InvalidRoleException when unrecognized role is requested")
    void register_unknownRole_throwsInvalidRoleException() {
        validRegisterRequest.setRole("SUPER_ADMIN");

        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(InvalidRoleException.class)
                .hasMessageContaining("Invalid role specified");

        verify(userRepository, never()).save(any(User.class));
    }

    // ================= LOGIN TESTS =================

    @Test
    @DisplayName("Successfully login with correct credentials and active account")
    void login_success() {
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("SecurePass123!", "hashed_bcrypt_password")).thenReturn(true);
        when(jwtService.generateToken(activeUser)).thenReturn("jwt.access.token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);
        when(refreshTokenService.createRefreshToken(activeUser)).thenReturn(
                RefreshTokenResult.builder()
                        .rawRefreshToken("mock-refresh-token")
                        .refreshToken(RefreshToken.builder().build())
                        .build()
        );

        AuthResult response = authService.login(validLoginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getRawRefreshToken()).isEqualTo("mock-refresh-token");
        assertThat(response.getLoginResponse()).isNotNull();
        assertThat(response.getLoginResponse().getAccessToken()).isEqualTo("jwt.access.token");
        assertThat(response.getLoginResponse().getTokenType()).isEqualTo("Bearer");
        assertThat(response.getLoginResponse().getExpiresIn()).isEqualTo(3600L);
        assertThat(response.getLoginResponse().getUser()).isNotNull();
        assertThat(response.getLoginResponse().getUser().getId()).isEqualTo(userId);
        assertThat(response.getLoginResponse().getUser().getEmail()).isEqualTo("john.doe@example.com");
        assertThat(response.getLoginResponse().getUser().getRole()).isEqualTo(Role.DEVELOPER);
        assertThat(response.getLoginResponse().getUser().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("Login normalizes email input case and whitespace")
    void login_normalizesEmail() {
        validLoginRequest.setEmail("  John.Doe@EXAMPLE.com  ");
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("SecurePass123!", "hashed_bcrypt_password")).thenReturn(true);
        when(jwtService.generateToken(activeUser)).thenReturn("jwt.token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);
        when(refreshTokenService.createRefreshToken(activeUser)).thenReturn(
                RefreshTokenResult.builder().rawRefreshToken("token").build()
        );

        AuthResult response = authService.login(validLoginRequest);

        assertThat(response.getLoginResponse().getAccessToken()).isEqualTo("jwt.token");
        verify(userRepository).findByEmail("john.doe@example.com");
    }

    @Test
    @DisplayName("Login fails with generic InvalidCredentialsException when email not found")
    void login_unknownEmail_throwsInvalidCredentialsException() {
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password.");

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(any(User.class));
        verify(refreshTokenService, never()).createRefreshToken(any(User.class));
    }

    @Test
    @DisplayName("Login fails with generic InvalidCredentialsException when password does not match")
    void login_incorrectPassword_throwsInvalidCredentialsException() {
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("SecurePass123!", "hashed_bcrypt_password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password.");

        verify(jwtService, never()).generateToken(any(User.class));
        verify(refreshTokenService, never()).createRefreshToken(any(User.class));
    }

    @Test
    @DisplayName("Login fails with AccountInactiveException when account status is INACTIVE")
    void login_inactiveAccount_throwsAccountInactiveException() {
        activeUser.setAccountStatus(AccountStatus.INACTIVE);
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("SecurePass123!", "hashed_bcrypt_password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(AccountInactiveException.class)
                .hasMessageContaining("inactive");

        verify(jwtService, never()).generateToken(any(User.class));
        verify(refreshTokenService, never()).createRefreshToken(any(User.class));
    }

    @Test
    @DisplayName("Login fails with AccountSuspendedException when account status is SUSPENDED")
    void login_suspendedAccount_throwsAccountSuspendedException() {
        activeUser.setAccountStatus(AccountStatus.SUSPENDED);
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("SecurePass123!", "hashed_bcrypt_password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(validLoginRequest))
                .isInstanceOf(AccountSuspendedException.class)
                .hasMessageContaining("suspended");

        verify(jwtService, never()).generateToken(any(User.class));
        verify(refreshTokenService, never()).createRefreshToken(any(User.class));
    }

    // ================= GET CURRENT USER TESTS =================

    @Test
    @DisplayName("Successfully get current user by ID")
    void getCurrentUser_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        UserResponse response = authService.getCurrentUser(userId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("Throws ResourceNotFoundException when user ID does not exist")
    void getCurrentUser_notFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }
}
