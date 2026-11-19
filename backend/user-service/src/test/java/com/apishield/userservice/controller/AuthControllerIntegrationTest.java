package com.apishield.userservice.controller;

import com.apishield.userservice.dto.request.RegisterRequest;
import com.apishield.userservice.entity.User;
import com.apishield.userservice.enums.AccountStatus;
import com.apishield.userservice.enums.Role;
import com.apishield.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should successfully register a new user and store encrypted password in database")
    void testSuccessfulRegistration() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Ada")
                .lastName("Lovelace")
                .email("ada.lovelace@apishield.io")
                .password("Str0ngP@ssword!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.lastName").value("Lovelace"))
                .andExpect(jsonPath("$.email").value("ada.lovelace@apishield.io"))
                .andExpect(jsonPath("$.role").value("DEVELOPER"))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        Optional<User> persisted = userRepository.findByEmail("ada.lovelace@apishield.io");
        assertThat(persisted).isPresent();
        User user = persisted.get();
        assertThat(user.getRole()).isEqualTo(Role.DEVELOPER);
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(user.getPasswordHash()).isNotEqualTo("Str0ngP@ssword!");
        assertThat(passwordEncoder.matches("Str0ngP@ssword!", user.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Should return 409 Conflict when attempting to register an existing email")
    void testDuplicateEmailRegistration() throws Exception {
        User existingUser = User.builder()
                .firstName("Alan")
                .lastName("Turing")
                .email("alan.turing@apishield.io")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .role(Role.DEVELOPER)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(false)
                .build();
        userRepository.save(existingUser);

        RegisterRequest duplicateRequest = RegisterRequest.builder()
                .firstName("Alan")
                .lastName("Turing")
                .email("alan.turing@apishield.io")
                .password("DifferentPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    @DisplayName("Should reject self-assigned ADMIN role with 400 Bad Request")
    void testRejectAdminRoleSelfAssignment() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Root")
                .lastName("Admin")
                .email("root.admin@apishield.io")
                .password("Str0ngP@ssword!")
                .role("ADMIN")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("does not allow self-assignment")));

        assertThat(userRepository.findByEmail("root.admin@apishield.io")).isEmpty();
    }

    @Test
    @DisplayName("Should reject self-assigned API_OWNER role with 400 Bad Request")
    void testRejectApiOwnerRoleSelfAssignment() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Owner")
                .lastName("User")
                .email("owner.user@apishield.io")
                .password("Str0ngP@ssword!")
                .role("API_OWNER")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("does not allow self-assignment")));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when email format is invalid")
    void testInvalidEmailFormat() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Test")
                .lastName("User")
                .email("invalid-email-address")
                .password("Str0ngP@ssword!")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when password does not meet complexity requirements")
    void testWeakPassword() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Test")
                .lastName("User")
                .email("valid.user@apishield.io")
                .password("simple")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    @DisplayName("Should deny access to unauthenticated non-public endpoints")
    void testProtectedEndpointAccessDenied() throws Exception {
        mockMvc.perform(get("/api/v1/users/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Actuator health endpoint should be publicly accessible")
    void testActuatorHealthIsAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
