package com.apishield.userservice.controller;

import com.apishield.userservice.dto.request.LoginRequest;
import com.apishield.userservice.dto.request.RegisterRequest;
import com.apishield.userservice.dto.response.AuthResult;
import com.apishield.userservice.dto.response.LoginResponse;
import com.apishield.userservice.dto.response.MessageResponse;
import com.apishield.userservice.dto.response.TokenRotationResult;
import com.apishield.userservice.dto.response.UserResponse;
import com.apishield.userservice.exception.InvalidRefreshTokenException;
import com.apishield.userservice.service.AuthService;
import com.apishield.userservice.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authService.login(request);
        ResponseCookie cookie = refreshTokenService.createRefreshTokenCookie(result.getRawRefreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(result.getLoginResponse());
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name = "${security.jwt.refresh-cookie-name:refresh_token}", required = false) String rawRefreshToken
    ) {
        if (rawRefreshToken == null || rawRefreshToken.trim().isEmpty()) {
            throw new InvalidRefreshTokenException("Refresh token cookie is missing.");
        }

        TokenRotationResult result = refreshTokenService.rotateRefreshToken(rawRefreshToken);
        ResponseCookie newCookie = refreshTokenService.createRefreshTokenCookie(result.getNewRawRefreshToken());

        LoginResponse responseBody = LoginResponse.builder()
                .accessToken(result.getNewAccessToken())
                .tokenType("Bearer")
                .expiresIn(result.getExpiresIn())
                .user(result.getUser())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, newCookie.toString())
                .body(responseBody);
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @CookieValue(name = "${security.jwt.refresh-cookie-name:refresh_token}", required = false) String rawRefreshToken
    ) {
        if (rawRefreshToken != null && !rawRefreshToken.trim().isEmpty()) {
            refreshTokenService.revokeRefreshToken(rawRefreshToken, "LOGOUT");
        }

        ResponseCookie cleanCookie = refreshTokenService.createCleanRefreshTokenCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
                .body(new MessageResponse("Logged out successfully."));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<MessageResponse> logoutAll(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        refreshTokenService.revokeAllUserSessions(userId, "LOGOUT_ALL");

        ResponseCookie cleanCookie = refreshTokenService.createCleanRefreshTokenCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
                .body(new MessageResponse("All active sessions have been logged out successfully."));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        UserResponse response = authService.getCurrentUser(userId);
        return ResponseEntity.ok(response);
    }
}
