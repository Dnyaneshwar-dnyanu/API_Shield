package com.apishield.userservice.controller;

import com.apishield.userservice.dto.request.DeactivateAccountRequest;
import com.apishield.userservice.dto.request.LoginRequest;
import com.apishield.userservice.dto.request.ResendVerificationRequest;
import com.apishield.userservice.dto.request.VerifyEmailRequest;
import com.apishield.userservice.entity.RefreshToken;
import com.apishield.userservice.entity.User;
import com.apishield.userservice.enums.AccountStatus;
import com.apishield.userservice.enums.Role;
import com.apishield.userservice.repository.EmailVerificationTokenRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        emailVerificationTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createTestUser(String email, String rawPassword, AccountStatus status, boolean verified) {
        return userRepository.save(User.builder()
                .firstName("Account")
                .lastName("Tester")
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(Role.DEVELOPER)
                .accountStatus(status)
                .emailVerified(verified)
                .build());
    }

    @Test
    @DisplayName("Email verification lifecycle activates inactive user and enables login")
    void testEmailVerificationLifecycle() throws Exception {
        User user = createTestUser("inactive@apishield.io", "Pass12345!", AccountStatus.INACTIVE, false);

        // 1. Request verification token
        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResendVerificationRequest("inactive@apishield.io"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("a new verification link has been sent")));

        // 2. Fetch raw token from dev store
        MvcResult devResult = mockMvc.perform(get("/api/v1/auth/dev/latest-verification-token")
                        .param("email", "inactive@apishield.io"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(devResult.getResponse().getContentAsString());
        String rawToken = json.get("verificationToken").asText();
        assertThat(rawToken).isNotBlank();

        // 3. Verify email
        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyEmailRequest(rawToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("Email verified successfully")));

        // 4. Verify in DB
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.isEmailVerified()).isTrue();
        assertThat(updated.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);

        // 5. Verify login now succeeds
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("inactive@apishield.io", "Pass12345!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    @DisplayName("Self-service account deactivation terminates sessions and immediately blocks JWT access")
    void testDeactivateAccountImmediateJwtRevocation() throws Exception {
        User user = createTestUser("deactivate@apishield.io", "Pass12345!", AccountStatus.ACTIVE, true);
        String token = jwtService.generateToken(user);

        // Pre-create refresh session
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash("hash-before-deactivation")
                .familyId(UUID.randomUUID())
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build());

        // Deactivate account
        mockMvc.perform(post("/api/v1/users/me/deactivate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeactivateAccountRequest("Pass12345!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("Account has been deactivated successfully")));

        // Verify user is INACTIVE in DB
        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getAccountStatus()).isEqualTo(AccountStatus.INACTIVE);

        // Verify refresh token is revoked
        RefreshToken session = refreshTokenRepository.findAll().get(0);
        assertThat(session.isRevoked()).isTrue();
        assertThat(session.getRevokeReason()).isEqualTo("ACCOUNT_DEACTIVATED");

        // SECURITY REQUIREMENT (Step 8.5):
        // Previously issued JWT is IMMEDIATELY blocked with 403 Forbidden without waiting for expiration!
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(containsString("Account is inactive")));
    }

    @Test
    @DisplayName("Suspended user JWT is immediately blocked with 403 Forbidden")
    void testSuspendedUserImmediateJwtBlock() throws Exception {
        User user = createTestUser("suspended@apishield.io", "Pass12345!", AccountStatus.ACTIVE, true);
        String token = jwtService.generateToken(user);

        // Suspend user directly in DB
        user.setAccountStatus(AccountStatus.SUSPENDED);
        user.setSuspensionReason("Security violation");
        userRepository.save(user);

        // Immediate check in JwtAuthenticationFilter returns 403 Forbidden
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(containsString("Account has been suspended")));
    }
}
