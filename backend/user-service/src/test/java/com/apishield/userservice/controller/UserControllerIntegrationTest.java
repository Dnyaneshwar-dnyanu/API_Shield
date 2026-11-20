package com.apishield.userservice.controller;

import com.apishield.userservice.dto.request.UpdateProfileRequest;
import com.apishield.userservice.entity.User;
import com.apishield.userservice.enums.AccountStatus;
import com.apishield.userservice.enums.Role;
import com.apishield.userservice.repository.RefreshTokenRepository;
import com.apishield.userservice.repository.UserRepository;
import com.apishield.userservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createTestUser(String email, String role) {
        return userRepository.save(User.builder()
                .firstName("Test")
                .lastName("Developer")
                .email(email)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .role(Role.valueOf(role))
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build());
    }

    @Test
    @DisplayName("GET /api/v1/users/me returns 401 when no JWT is provided")
    void testGetProfileWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("GET /api/v1/users/me returns 200 with profile data and no password hash")
    void testGetProfileWithValidToken() throws Exception {
        User user = createTestUser("profile@apishield.io", "DEVELOPER");
        String token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("Developer"))
                .andExpect(jsonPath("$.email").value("profile@apishield.io"))
                .andExpect(jsonPath("$.role").value("DEVELOPER"))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.emailVerified").value(true))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("PUT /api/v1/users/me returns 200 and updates first and last name")
    void testUpdateProfileWithValidData() throws Exception {
        User user = createTestUser("update@apishield.io", "DEVELOPER");
        String token = jwtService.generateToken(user);

        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                .firstName("UpdatedFirst")
                .lastName("UpdatedLast")
                .build();

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.firstName").value("UpdatedFirst"))
                .andExpect(jsonPath("$.lastName").value("UpdatedLast"))
                .andExpect(jsonPath("$.email").value("update@apishield.io"));

        // Verify persistence in repository
        User persisted = userRepository.findById(user.getId()).orElseThrow();
        assertThat(persisted.getFirstName()).isEqualTo("UpdatedFirst");
        assertThat(persisted.getLastName()).isEqualTo("UpdatedLast");
    }

    @Test
    @DisplayName("PUT /api/v1/users/me returns 400 when first or last name is blank")
    void testUpdateProfileWithInvalidData() throws Exception {
        User user = createTestUser("invalid-update@apishield.io", "DEVELOPER");
        String token = jwtService.generateToken(user);

        UpdateProfileRequest blankFirst = UpdateProfileRequest.builder()
                .firstName("   ")
                .lastName("ValidLast")
                .build();

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankFirst)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.firstName").exists());

        UpdateProfileRequest blankLast = UpdateProfileRequest.builder()
                .firstName("ValidFirst")
                .lastName("")
                .build();

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankLast)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.lastName").exists());
    }

    @Test
    @DisplayName("PUT /api/v1/users/me ignores attempts to alter role, email, status, or id (Mass Assignment Protection)")
    void testUpdateProfileMassAssignmentProtection() throws Exception {
        User user = createTestUser("mass-assignment@apishield.io", "DEVELOPER");
        String token = jwtService.generateToken(user);

        // Attempting to send JSON payload with extra privileged fields
        String maliciousPayload = """
                {
                    "firstName": "Hacker",
                    "lastName": "User",
                    "role": "ADMIN",
                    "email": "hacked@apishield.io",
                    "accountStatus": "SUSPENDED",
                    "emailVerified": false,
                    "id": "%s"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(maliciousPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Hacker"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.role").value("DEVELOPER"))
                .andExpect(jsonPath("$.email").value("mass-assignment@apishield.io"))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.emailVerified").value(true));

        // In DB, role and email remain untouched
        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getRole()).isEqualTo(Role.DEVELOPER);
        assertThat(reloaded.getEmail()).isEqualTo("mass-assignment@apishield.io");
        assertThat(reloaded.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("GET /api/v1/users/me returns 404 if authenticated user was deleted")
    void testGetProfileUserNotFound() throws Exception {
        User user = createTestUser("temp@apishield.io", "DEVELOPER");
        String token = jwtService.generateToken(user);

        // Delete user from DB
        userRepository.deleteById(user.getId());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(containsString("User not found")));
    }
}
