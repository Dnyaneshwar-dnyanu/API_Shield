package com.apishield.userservice.controller;

import com.apishield.userservice.dto.request.LoginRequest;
import com.apishield.userservice.dto.request.RegisterRequest;
import com.apishield.userservice.entity.User;
import com.apishield.userservice.enums.AccountStatus;
import com.apishield.userservice.enums.Role;
import com.apishield.userservice.repository.UserRepository;
import com.apishield.userservice.security.JwtService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
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
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    // ================= REGISTRATION TESTS =================

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

    // ================= LOGIN TESTS =================

    @Test
    @DisplayName("Should successfully login with valid credentials and return JWT access token")
    void testSuccessfulLogin() throws Exception {
        User user = User.builder()
                .firstName("Grace")
                .lastName("Hopper")
                .email("grace.hopper@apishield.io")
                .passwordHash(passwordEncoder.encode("SecureP@ss123"))
                .role(Role.DEVELOPER)
                .accountStatus(AccountStatus.ACTIVE)
                .emailVerified(true)
                .build();
        userRepository.save(user);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("grace.hopper@apishield.io")
                .password("SecureP@ss123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.user.email").value("grace.hopper@apishield.io"))
                .andExpect(jsonPath("$.user.role").value("DEVELOPER"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Should return 401 Unauthorized with generic message on incorrect password")
    void testLoginIncorrectPassword() throws Exception {
        User user = User.builder()
                .firstName("Grace")
                .lastName("Hopper")
                .email("grace.hopper@apishield.io")
                .passwordHash(passwordEncoder.encode("CorrectP@ss123"))
                .role(Role.DEVELOPER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        userRepository.save(user);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("grace.hopper@apishield.io")
                .password("WrongPassword123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    @DisplayName("Should return generic 401 Unauthorized when logging in with unknown email")
    void testLoginUnknownEmail() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("nonexistent.user@apishield.io")
                .password("AnyPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    @DisplayName("Should return 403 Forbidden when account is INACTIVE")
    void testLoginInactiveAccount() throws Exception {
        User user = User.builder()
                .firstName("Inactive")
                .lastName("User")
                .email("inactive@apishield.io")
                .passwordHash(passwordEncoder.encode("SecureP@ss123"))
                .role(Role.DEVELOPER)
                .accountStatus(AccountStatus.INACTIVE)
                .build();
        userRepository.save(user);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("inactive@apishield.io")
                .password("SecureP@ss123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(containsString("inactive")));
    }

    @Test
    @DisplayName("Should return 403 Forbidden when account is SUSPENDED")
    void testLoginSuspendedAccount() throws Exception {
        User user = User.builder()
                .firstName("Suspended")
                .lastName("User")
                .email("suspended@apishield.io")
                .passwordHash(passwordEncoder.encode("SecureP@ss123"))
                .role(Role.DEVELOPER)
                .accountStatus(AccountStatus.SUSPENDED)
                .build();
        userRepository.save(user);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("suspended@apishield.io")
                .password("SecureP@ss123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(containsString("suspended")));
    }

    // ================= PROTECTED ENDPOINT & JWT AUTHENTICATION TESTS =================

    @Test
    @DisplayName("Should successfully access protected endpoint with valid JWT bearer token")
    void testProtectedEndpointWithValidJwt() throws Exception {
        User user = User.builder()
                .firstName("Linus")
                .lastName("Torvalds")
                .email("linus@apishield.io")
                .passwordHash(passwordEncoder.encode("KernelSecure123!"))
                .role(Role.DEVELOPER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUser.getId().toString()))
                .andExpect(jsonPath("$.email").value("linus@apishield.io"))
                .andExpect(jsonPath("$.role").value("DEVELOPER"));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when accessing protected endpoint without bearer token")
    void testProtectedEndpointWithMissingBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when accessing protected endpoint with expired JWT")
    void testProtectedEndpointWithExpiredJwt() throws Exception {
        Instant now = Instant.now();
        Algorithm algorithm = Algorithm.HMAC256("t3st-s3cur3-256-b1t-jwt-s3cr3t-k3y-f0r-un1t-and-1nt3gr4t10n-t3st1ng!".getBytes(StandardCharsets.UTF_8));
        String expiredToken = JWT.create()
                .withIssuer("apishield-user-service")
                .withSubject("11111111-1111-1111-1111-111111111111")
                .withClaim("email", "expired@apishield.io")
                .withClaim("role", "DEVELOPER")
                .withIssuedAt(Date.from(now.minusSeconds(3600)))
                .withExpiresAt(Date.from(now.minusSeconds(1800)))
                .sign(algorithm);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when accessing protected endpoint with invalid signature")
    void testProtectedEndpointWithInvalidSignature() throws Exception {
        Algorithm wrongAlgorithm = Algorithm.HMAC256("wrong-secret-key-that-does-not-match-at-all-for-testing!".getBytes(StandardCharsets.UTF_8));
        String tamperedToken = JWT.create()
                .withIssuer("apishield-user-service")
                .withSubject("11111111-1111-1111-1111-111111111111")
                .withClaim("email", "tampered@apishield.io")
                .withClaim("role", "DEVELOPER")
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(Date.from(Instant.now().plusSeconds(3600)))
                .sign(wrongAlgorithm);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Actuator health endpoint should remain publicly accessible without authentication")
    void testActuatorHealthIsAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
