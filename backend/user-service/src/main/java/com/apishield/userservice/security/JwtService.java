package com.apishield.userservice.security;

import com.apishield.userservice.entity.User;
import com.apishield.userservice.enums.Role;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;
    private Algorithm algorithm;
    private JWTVerifier verifier;

    @PostConstruct
    public void init() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT signing secret (security.jwt.secret) must not be empty.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT signing secret must be at least 256 bits (32 bytes) for HMAC-SHA256 security.");
        }

        this.algorithm = Algorithm.HMAC256(secret.getBytes(StandardCharsets.UTF_8));
        this.verifier = JWT.require(this.algorithm)
                .withIssuer(jwtProperties.getIssuer())
                .build();
    }

    public String generateToken(User user) {
        return generateToken(user.getId(), user.getEmail(), user.getRole(), jwtProperties.getExpirationSeconds());
    }

    public String generateToken(UUID userId, String email, Role role, long expirationSeconds) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirationSeconds);

        return JWT.create()
                .withIssuer(jwtProperties.getIssuer())
                .withSubject(userId.toString())
                .withClaim("email", email)
                .withClaim("role", role.name())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiry))
                .sign(this.algorithm);
    }

    public DecodedJWT verifyAndDecode(String token) throws JWTVerificationException {
        return verifier.verify(token);
    }

    public boolean validateToken(String token) {
        try {
            verifyAndDecode(token);
            return true;
        } catch (JWTVerificationException ex) {
            log.debug("JWT token validation failed: {}", ex.getMessage());
            return false;
        }
    }

    public UUID extractUserId(String token) {
        DecodedJWT decoded = verifyAndDecode(token);
        return UUID.fromString(decoded.getSubject());
    }

    public String extractEmail(String token) {
        DecodedJWT decoded = verifyAndDecode(token);
        return decoded.getClaim("email").asString();
    }

    public Role extractRole(String token) {
        DecodedJWT decoded = verifyAndDecode(token);
        return Role.valueOf(decoded.getClaim("role").asString());
    }

    public long getExpirationSeconds() {
        return jwtProperties.getExpirationSeconds();
    }

    public String getIssuer() {
        return jwtProperties.getIssuer();
    }
}
