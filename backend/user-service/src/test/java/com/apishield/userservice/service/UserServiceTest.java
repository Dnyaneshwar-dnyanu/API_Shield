package com.apishield.userservice.service;

import com.apishield.userservice.dto.request.UpdateProfileRequest;
import com.apishield.userservice.dto.response.UserResponse;
import com.apishield.userservice.entity.User;
import com.apishield.userservice.enums.AccountStatus;
import com.apishield.userservice.enums.Role;
import com.apishield.userservice.exception.ResourceNotFoundException;
import com.apishield.userservice.repository.UserRepository;
import com.apishield.userservice.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(userId)
                .firstName("Ada")
                .lastName("Lovelace")
                .email("ada@apishield.io")
                .passwordHash("hashed-password-123")
                .role(Role.DEVELOPER)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .createdAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("getProfile successfully returns user response and does not expose password hash")
    void getProfile_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        UserResponse response = userService.getProfile(userId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getFirstName()).isEqualTo("Ada");
        assertThat(response.getLastName()).isEqualTo("Lovelace");
        assertThat(response.getEmail()).isEqualTo("ada@apishield.io");
        assertThat(response.getRole()).isEqualTo(Role.DEVELOPER);
        assertThat(response.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(response.isEmailVerified()).isTrue();
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("getProfile throws ResourceNotFoundException when user does not exist")
    void getProfile_userNotFound_throwsException() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(unknownId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with ID");
    }

    @Test
    @DisplayName("updateProfile normalizes input, updates first and last name, and returns updated profile")
    void updateProfile_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName("  Augusta Ada  ")
                .lastName("  King-Noel  ")
                .build();

        UserResponse response = userService.updateProfile(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getFirstName()).isEqualTo("Augusta Ada");
        assertThat(response.getLastName()).isEqualTo("King-Noel");
        assertThat(sampleUser.getFirstName()).isEqualTo("Augusta Ada");
        assertThat(sampleUser.getLastName()).isEqualTo("King-Noel");
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("updateProfile throws ResourceNotFoundException when user does not exist")
    void updateProfile_userNotFound_throwsException() {
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName("New")
                .lastName("Name")
                .build();

        assertThatThrownBy(() -> userService.updateProfile(unknownId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with ID");
    }
}
