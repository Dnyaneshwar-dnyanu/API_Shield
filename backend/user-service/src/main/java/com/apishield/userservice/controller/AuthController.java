package com.apishield.userservice.controller;

import com.apishield.userservice.dto.request.ForgotPasswordRequest;
import com.apishield.userservice.dto.request.LoginRequest;
import com.apishield.userservice.dto.request.RegisterRequest;
import com.apishield.userservice.dto.request.ResendVerificationRequest;
import com.apishield.userservice.dto.request.ResetPasswordRequest;
import com.apishield.userservice.dto.request.VerifyEmailRequest;
import com.apishield.userservice.dto.response.AuthResult;
import com.apishield.userservice.dto.response.LoginResponse;
import com.apishield.userservice.dto.response.MessageResponse;
import com.apishield.userservice.dto.response.TokenRotationResult;
import com.apishield.userservice.dto.response.UserResponse;
import com.apishield.userservice.exception.InvalidRefreshTokenException;
import com.apishield.userservice.service.AccountService;
import com.apishield.userservice.service.AuthService;
import com.apishield.userservice.service.NotificationService;
import com.apishield.userservice.service.PasswordService;
import com.apishield.userservice.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordService passwordService;
    private final NotificationService notificationService;
    private final AccountService accountService;

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

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordService.forgotPassword(request);
        return ResponseEntity.ok(new MessageResponse("If an account with that email exists, a password reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Password reset successfully. All active sessions have been invalidated. Please log in with your new password."));
    }

    /**
     * Development testing hook to retrieve the latest raw password-reset token for a given email address.
     * In production, the Notification Service delivers this out-of-band via email.
     */
    @GetMapping("/dev/latest-reset-token")
    public ResponseEntity<Map<String, String>> getDevResetToken(@RequestParam String email) {
        String token = notificationService.getLatestDevResetToken(email);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No active reset token found in development store for " + email));
        }
        return ResponseEntity.ok(Map.of("email", email, "resetToken", token));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        accountService.verifyEmail(request);
        return ResponseEntity.ok(new MessageResponse("Email verified successfully. Your account is now active."));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<MessageResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        accountService.resendVerificationEmail(request);
        return ResponseEntity.ok(new MessageResponse("If the account exists and is not yet verified, a new verification link has been sent."));
    }

    @GetMapping("/dev/latest-verification-token")
    public ResponseEntity<Map<String, String>> getDevVerificationToken(@RequestParam String email) {
        String token = notificationService.getLatestDevVerificationToken(email);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No active verification token found in development store for " + email));
        }
        return ResponseEntity.ok(Map.of("email", email, "verificationToken", token));
    }
}
