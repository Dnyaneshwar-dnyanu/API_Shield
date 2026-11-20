package com.apishield.userservice.controller;

import com.apishield.userservice.dto.request.LoginRequest;
import com.apishield.userservice.dto.request.RegisterRequest;
import com.apishield.userservice.entity.RefreshToken;
import com.apishield.userservice.entity.User;
import com.apishield.userservice.enums.AccountStatus;
import com.apishield.userservice.enums.Role;
import com.apishield.userservice.repository.RefreshTokenRepository;
import com.apishield.userservice.repository.UserRepository;
import com.apishield.userservice.security.JwtService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.servlet.http.Cookie;
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
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
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

    // ================= LOGIN & REFRESH TOKEN TESTS =================

    @Test
    @DisplayName("Should login successfully, set HttpOnly refresh cookie, store SHA-256 hash in DB, and omit token from JSON")
    void testSuccessfulLogin() throws Exception {
        User user = createTestUser("grace.hopper@apishield.io", "SecureP@ss123", AccountStatus.ACTIVE);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("grace.hopper@apishield.io")
                .password("SecureP@ss123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.refreshToken").doesNotExist()) // NEVER in JSON!
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(cookie().path("refresh_token", "/api/v1/auth"))
                .andReturn();

        jakarta.servlet.http.Cookie refreshCookie = result.getResponse().getCookie("refresh_token");
        assertThat(refreshCookie).isNotNull();
        String rawRefreshToken = refreshCookie.getValue();
        assertThat(rawRefreshToken).isNotBlank();

        // Verify that database DOES NOT store plaintext, but stores 64-char SHA-256 hash
        List<RefreshToken> tokens = refreshTokenRepository.findAll();
        assertThat(tokens).hasSize(1);
        RefreshToken stored = tokens.get(0);
        assertThat(stored.getTokenHash()).isNotEqualTo(rawRefreshToken);
        assertThat(stored.getTokenHash()).isEqualTo(hashSha256(rawRefreshToken));
        assertThat(stored.getUser().getId()).isEqualTo(user.getId());
        assertThat(stored.getFamilyId()).isNotNull();
    }

    @Test
    @DisplayName("Should return 401 Unauthorized with generic message on incorrect password")
    void testLoginIncorrectPassword() throws Exception {
        createTestUser("grace.hopper@apishield.io", "CorrectP@ss123", AccountStatus.ACTIVE);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("grace.hopper@apishield.io")
                .password("WrongPassword123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password."))
                .andExpect(cookie().doesNotExist("refresh_token"));
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
        createTestUser("inactive@apishield.io", "SecureP@ss123", AccountStatus.INACTIVE);

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
        createTestUser("suspended@apishield.io", "SecureP@ss123", AccountStatus.SUSPENDED);

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

    // ================= ROTATION & REUSE DETECTION TESTS =================

    @Test
    @DisplayName("Refresh with valid cookie rotates refresh token, returns new access token, and sets new cookie")
    void testSuccessfulTokenRotation() throws Exception {
        createTestUser("rotator@apishield.io", "Pass12345!", AccountStatus.ACTIVE);

        // 1. Initial Login
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("rotator@apishield.io", "Pass12345!"))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie initialCookie = loginResult.getResponse().getCookie("refresh_token");
        assertThat(initialCookie).isNotNull();
        String firstRefreshToken = initialCookie.getValue();

        // 2. Perform Refresh
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(initialCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(cookie().exists("refresh_token"))
                .andReturn();

        Cookie rotatedCookie = refreshResult.getResponse().getCookie("refresh_token");
        assertThat(rotatedCookie).isNotNull();
        String secondRefreshToken = rotatedCookie.getValue();

        // Rotated token must have a DIFFERENT value
        assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);

        // Verify DB state: first token marked ROTATED, second token active with SAME family_id
        RefreshToken firstStored = refreshTokenRepository.findByTokenHash(hashSha256(firstRefreshToken)).orElseThrow();
        RefreshToken secondStored = refreshTokenRepository.findByTokenHash(hashSha256(secondRefreshToken)).orElseThrow();

        assertThat(firstStored.getRevokedAt()).isNotNull();
        assertThat(firstStored.getRevokeReason()).isEqualTo("ROTATED");
        assertThat(firstStored.getReplacedByTokenId()).isEqualTo(secondStored.getId());

        assertThat(secondStored.getRevokedAt()).isNull();
        assertThat(secondStored.getFamilyId()).isEqualTo(firstStored.getFamilyId());
    }

    @Test
    @DisplayName("Reuse detection: Presenting previously rotated token revokes entire family and returns 401")
    void testReuseDetectionRevokesFamily() throws Exception {
        createTestUser("reuse@apishield.io", "Pass12345!", AccountStatus.ACTIVE);

        // 1. Login -> gets Token A
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("reuse@apishield.io", "Pass12345!"))))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookieA = loginResult.getResponse().getCookie("refresh_token");

        // 2. Legitimate Refresh -> Token A rotates to Token B
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookieA))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookieB = refreshResult.getResponse().getCookie("refresh_token");

        // 3. MALICIOUS ATTACK: Replay old Token A
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookieA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(containsString("reuse detected")));

        // 4. Verify that Token B was also invalidated due to family revocation!
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookieB))
                .andExpect(status().isUnauthorized());

        // Check in DB that all tokens in this family are revoked with REUSE_DETECTED
        List<RefreshToken> familyTokens = refreshTokenRepository.findAll();
        for (RefreshToken token : familyTokens) {
            assertThat(token.isRevoked()).isTrue();
        }
    }

    @Test
    @DisplayName("Should return 401 when refresh cookie is missing")
    void testRefreshMissingCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(containsString("missing")));
    }

    @Test
    @DisplayName("Should return 401 when refresh token is expired")
    void testRefreshExpiredToken() throws Exception {
        User user = createTestUser("expired@apishield.io", "Pass12345!", AccountStatus.ACTIVE);
        String rawToken = "expired-raw-token";
        RefreshToken expiredToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashSha256(rawToken))
                .familyId(UUID.randomUUID())
                .createdAt(Instant.now().minusSeconds(1000))
                .expiresAt(Instant.now().minusSeconds(10))
                .build();
        refreshTokenRepository.save(expiredToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", rawToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(containsString("expired")));
    }

    // ================= LOGOUT TESTS =================

    @Test
    @DisplayName("Logout revokes refresh token session and clears cookie")
    void testLogoutRevokesSessionAndClearsCookie() throws Exception {
        createTestUser("logout@apishield.io", "Pass12345!", AccountStatus.ACTIVE);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("logout@apishield.io", "Pass12345!"))))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refreshCookie = loginResult.getResponse().getCookie("refresh_token");

        MvcResult logoutResult = mockMvc.perform(post("/api/v1/auth/logout").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully."))
                .andReturn();

        // Cookie is cleared with Max-Age 0
        Cookie clearedCookie = logoutResult.getResponse().getCookie("refresh_token");
        assertThat(clearedCookie).isNotNull();
        assertThat(clearedCookie.getMaxAge()).isEqualTo(0);

        // In DB, token is marked LOGOUT
        RefreshToken token = refreshTokenRepository.findByTokenHash(hashSha256(refreshCookie.getValue())).orElseThrow();
        assertThat(token.isRevoked()).isTrue();
        assertThat(token.getRevokeReason()).isEqualTo("LOGOUT");
    }

    @Test
    @DisplayName("Repeated logout requests are safe and idempotent")
    void testRepeatedLogoutIsIdempotent() throws Exception {
        // Logout without any cookie
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully."))
                .andExpect(cookie().exists("refresh_token"));
    }

    @Test
    @DisplayName("Logout-all endpoint revokes all active sessions for authenticated user")
    void testLogoutAllSessions() throws Exception {
        User user = createTestUser("multi@apishield.io", "Pass12345!", AccountStatus.ACTIVE);

        // Create 2 refresh token sessions for the user
        String token1 = "token-1";
        String token2 = "token-2";
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hashSha256(token1))
                .familyId(UUID.randomUUID())
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(1000))
                .build());
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hashSha256(token2))
                .familyId(UUID.randomUUID())
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(1000))
                .build());

        String accessJwt = jwtService.generateToken(user);

        mockMvc.perform(post("/api/v1/auth/logout-all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("All active sessions have been logged out")));

        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(user.getId());
        assertThat(activeTokens).isEmpty();
    }

    // ================= PROTECTED ENDPOINTS & HEALTH =================

    @Test
    @DisplayName("Should successfully access protected endpoint with valid JWT bearer token")
    void testProtectedEndpointWithValidJwt() throws Exception {
        User user = createTestUser("linus@apishield.io", "KernelSecure123!", AccountStatus.ACTIVE);
        String token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
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
    @DisplayName("Actuator health endpoint should remain publicly accessible without authentication")
    void testActuatorHealthIsAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    private User createTestUser(String email, String password, AccountStatus status) {
        User user = User.builder()
                .firstName("Test")
                .lastName("User")
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(Role.DEVELOPER)
                .accountStatus(status)
                .emailVerified(true)
                .build();
        return userRepository.save(user);
    }

    private String hashSha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
