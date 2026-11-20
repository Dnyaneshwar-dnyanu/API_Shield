package com.apishield.userservice.controller;

import com.apishield.userservice.dto.request.ChangePasswordRequest;
import com.apishield.userservice.dto.request.ForgotPasswordRequest;
import com.apishield.userservice.dto.request.LoginRequest;
import com.apishield.userservice.dto.request.ResetPasswordRequest;
import com.apishield.userservice.entity.RefreshToken;
import com.apishield.userservice.entity.User;
import com.apishield.userservice.enums.AccountStatus;
import com.apishield.userservice.enums.Role;
import com.apishield.userservice.repository.PasswordResetTokenRepository;
import com.apishield.userservice.repository.RefreshTokenRepository;
import com.apishield.userservice.repository.UserRepository;
import com.apishield.userservice.security.JwtService;
import com.apishield.userservice.service.NotificationService;
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
class PasswordIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private NotificationService notificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createTestUser(String email, String rawPassword) {
        return userRepository.save(User.builder()
                .firstName("Password")
                .lastName("Tester")
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(Role.DEVELOPER)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build());
    }

    @Test
    @DisplayName("Change password successfully updates password and revokes all refresh sessions")
    void testChangePasswordSuccess() throws Exception {
        User user = createTestUser("changepass@apishield.io", "OldP@ssword123");
        String token = jwtService.generateToken(user);

        // Pre-create an active refresh token session for user
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash("pre-existing-hash")
                .familyId(UUID.randomUUID())
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build());

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("OldP@ssword123")
                .newPassword("NewP@ssword456!")
                .confirmNewPassword("NewP@ssword456!")
                .build();

        mockMvc.perform(post("/api/v1/users/me/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("Password changed successfully")));

        // Verify old password fails to log in
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("changepass@apishield.io", "OldP@ssword123"))))
                .andExpect(status().isUnauthorized());

        // Verify new password logs in successfully
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("changepass@apishield.io", "NewP@ssword456!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());

        // Verify previous refresh sessions were revoked
        RefreshToken session = refreshTokenRepository.findAll().get(0);
        assertThat(session.isRevoked()).isTrue();
        assertThat(session.getRevokeReason()).isEqualTo("PASSWORD_CHANGE");
    }

    @Test
    @DisplayName("Change password fails with 401 when current password is wrong")
    void testChangePasswordWrongCurrent() throws Exception {
        User user = createTestUser("wrongpass@apishield.io", "CorrectP@ss1");
        String token = jwtService.generateToken(user);

        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("IncorrectP@ss1")
                .newPassword("NewP@ssword456!")
                .confirmNewPassword("NewP@ssword456!")
                .build();

        mockMvc.perform(post("/api/v1/users/me/change-password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("Current password is incorrect")));
    }

    @Test
    @DisplayName("Forgot password and reset password complete lifecycle works and prevents token reuse")
    void testForgotPasswordAndResetPasswordLifecycle() throws Exception {
        User user = createTestUser("forgotpass@apishield.io", "InitialP@ss123");

        // 1. Request forgot password
        ForgotPasswordRequest forgotRequest = ForgotPasswordRequest.builder()
                .email("forgotpass@apishield.io")
                .build();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("a password reset link has been sent")));

        // 2. Fetch raw reset token from dev endpoint
        MvcResult devResult = mockMvc.perform(get("/api/v1/auth/dev/latest-reset-token")
                        .param("email", "forgotpass@apishield.io"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(devResult.getResponse().getContentAsString());
        String rawResetToken = json.get("resetToken").asText();
        assertThat(rawResetToken).isNotBlank();

        // 3. Reset password using the valid token
        ResetPasswordRequest resetRequest = ResetPasswordRequest.builder()
                .token(rawResetToken)
                .newPassword("BrandNewP@ssword789!")
                .confirmNewPassword("BrandNewP@ssword789!")
                .build();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("Password reset successfully")));

        // 4. Verify login with new password succeeds and old password fails
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("forgotpass@apishield.io", "InitialP@ss123"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("forgotpass@apishield.io", "BrandNewP@ssword789!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());

        // 5. Attempting to reuse the same reset token fails with 400
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("already been used")));
    }

    @Test
    @DisplayName("Forgot password with nonexistent email returns identical 200 response to prevent user enumeration")
    void testForgotPasswordNonexistentEmail() throws Exception {
        ForgotPasswordRequest forgotRequest = ForgotPasswordRequest.builder()
                .email("nonexistent-email@apishield.io")
                .build();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("a password reset link has been sent")));
    }
}
