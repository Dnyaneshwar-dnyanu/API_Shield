package com.apishield.userservice.service;

import com.apishield.userservice.dto.request.RegisterRequest;
import com.apishield.userservice.dto.response.UserResponse;
import com.apishield.userservice.entity.User;
import com.apishield.userservice.enums.AccountStatus;
import com.apishield.userservice.enums.Role;
import com.apishield.userservice.exception.DuplicateEmailException;
import com.apishield.userservice.exception.InvalidRoleException;
import com.apishield.userservice.repository.UserRepository;
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

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .password("SecurePass123!")
                .role(null)
                .build();
    }

    @Test
    @DisplayName("Successfully register developer when role is not specified")
    void register_success_defaultRole() {
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("hashed_password_xyz");

        UUID generatedId = UUID.randomUUID();
        Instant now = Instant.now();
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(generatedId);
            u.setCreatedAt(now);
            return u;
        });

        UserResponse response = authService.register(validRequest);

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
        assertThat(saved.getPasswordHash()).isEqualTo("hashed_password_xyz");
        assertThat(saved.getRole()).isEqualTo(Role.DEVELOPER);
    }

    @Test
    @DisplayName("Successfully register developer when DEVELOPER role is explicitly requested")
    void register_success_explicitDeveloperRole() {
        validRequest.setRole("DEVELOPER");
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = authService.register(validRequest);

        assertThat(response.getRole()).isEqualTo(Role.DEVELOPER);
    }

    @Test
    @DisplayName("Email is normalized by trimming whitespace and converting to lowercase")
    void register_normalizesEmail() {
        validRequest.setEmail("  John.DOE@ExAMple.COM  ");
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = authService.register(validRequest);

        assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
        verify(userRepository).existsByEmail("john.doe@example.com");
    }

    @Test
    @DisplayName("Throws DuplicateEmailException when email already exists")
    void register_duplicateEmail_throwsException() {
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Throws InvalidRoleException when client attempts to self-assign ADMIN role")
    void register_adminRole_throwsInvalidRoleException() {
        validRequest.setRole("ADMIN");

        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(InvalidRoleException.class)
                .hasMessageContaining("does not allow self-assignment");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Throws InvalidRoleException when client attempts to self-assign API_OWNER role")
    void register_apiOwnerRole_throwsInvalidRoleException() {
        validRequest.setRole("API_OWNER");

        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(InvalidRoleException.class)
                .hasMessageContaining("does not allow self-assignment");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Throws InvalidRoleException when unrecognized role is requested")
    void register_unknownRole_throwsInvalidRoleException() {
        validRequest.setRole("SUPER_ADMIN");

        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(InvalidRoleException.class)
                .hasMessageContaining("Invalid role specified");

        verify(userRepository, never()).save(any(User.class));
    }
}
