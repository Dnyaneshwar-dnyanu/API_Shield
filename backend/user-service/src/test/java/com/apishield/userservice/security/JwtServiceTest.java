package com.apishield.userservice.security;

import com.apishield.userservice.entity.User;
import com.apishield.userservice.enums.AccountStatus;
import com.apishield.userservice.enums.Role;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String VALID_SECRET = "this-is-a-secure-256-bit-test-key-with-sufficient-length!";
    private static final String ISSUER = "apishield-user-service";

    private JwtService jwtService;
    private JwtProperties jwtProperties;
    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(VALID_SECRET);
        jwtProperties.setIssuer(ISSUER);
        jwtProperties.setExpirationSeconds(3600);

        jwtService = new JwtService(jwtProperties);
        jwtService.init();

        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("developer@apishield.io")
                .role(Role.DEVELOPER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Generated token contains expected subject, issuer, email, role and expiration")
    void generateToken_containsExpectedClaims() {
        String token = jwtService.generateToken(testUser);

        assertThat(token).isNotBlank();
        DecodedJWT decoded = jwtService.verifyAndDecode(token);

        assertThat(decoded.getSubject()).isEqualTo(userId.toString());
        assertThat(decoded.getIssuer()).isEqualTo(ISSUER);
        assertThat(decoded.getClaim("email").asString()).isEqualTo("developer@apishield.io");
        assertThat(decoded.getClaim("role").asString()).isEqualTo("DEVELOPER");
        assertThat(decoded.getExpiresAt()).isAfter(Date.from(Instant.now()));
        assertThat(decoded.getIssuedAt()).isNotNull();
    }

    @Test
    @DisplayName("Validation succeeds for a valid token")
    void validateToken_validToken_returnsTrue() {
        String token = jwtService.generateToken(testUser);
        assertThat(jwtService.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("Extracts claims accurately from valid token")
    void extractClaims_accurate() {
        String token = jwtService.generateToken(testUser);

        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtService.extractEmail(token)).isEqualTo("developer@apishield.io");
        assertThat(jwtService.extractRole(token)).isEqualTo(Role.DEVELOPER);
    }

    @Test
    @DisplayName("Expired token fails validation and throws TokenExpiredException on decode")
    void validateToken_expiredToken_fails() {
        // Generate token with negative expiration (-60 seconds)
        Instant now = Instant.now();
        Algorithm algorithm = Algorithm.HMAC256(VALID_SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = JWT.create()
                .withIssuer(ISSUER)
                .withSubject(userId.toString())
                .withClaim("email", testUser.getEmail())
                .withClaim("role", testUser.getRole().name())
                .withIssuedAt(Date.from(now.minusSeconds(120)))
                .withExpiresAt(Date.from(now.minusSeconds(60)))
                .sign(algorithm);

        assertThat(jwtService.validateToken(expiredToken)).isFalse();
        assertThatThrownBy(() -> jwtService.verifyAndDecode(expiredToken))
                .isInstanceOf(TokenExpiredException.class);
    }

    @Test
    @DisplayName("Token signed with different secret is rejected with SignatureVerificationException")
    void validateToken_invalidSignature_fails() {
        Algorithm differentAlgorithm = Algorithm.HMAC256("different-secret-key-that-does-not-match-at-all!".getBytes(StandardCharsets.UTF_8));
        String tamperedToken = JWT.create()
                .withIssuer(ISSUER)
                .withSubject(userId.toString())
                .withClaim("email", testUser.getEmail())
                .withClaim("role", testUser.getRole().name())
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(Date.from(Instant.now().plusSeconds(3600)))
                .sign(differentAlgorithm);

        assertThat(jwtService.validateToken(tamperedToken)).isFalse();
        assertThatThrownBy(() -> jwtService.verifyAndDecode(tamperedToken))
                .isInstanceOf(SignatureVerificationException.class);
    }

    @Test
    @DisplayName("Initialization fails with clear IllegalStateException if secret is null or blank")
    void init_missingSecret_throwsException() {
        JwtProperties emptyProps = new JwtProperties();
        emptyProps.setSecret("");
        JwtService service = new JwtService(emptyProps);

        assertThatThrownBy(service::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    @DisplayName("Initialization fails with clear IllegalStateException if secret is less than 32 characters (256 bits)")
    void init_shortSecret_throwsException() {
        JwtProperties shortProps = new JwtProperties();
        shortProps.setSecret("too-short-secret");
        JwtService service = new JwtService(shortProps);

        assertThatThrownBy(service::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 256 bits");
    }
}
