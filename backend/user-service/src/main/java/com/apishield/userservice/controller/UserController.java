package com.apishield.userservice.controller;

import com.apishield.userservice.dto.request.ChangePasswordRequest;
import com.apishield.userservice.dto.request.DeactivateAccountRequest;
import com.apishield.userservice.dto.request.UpdateProfileRequest;
import com.apishield.userservice.dto.response.MessageResponse;
import com.apishield.userservice.dto.response.UserResponse;
import com.apishield.userservice.service.AccountService;
import com.apishield.userservice.service.PasswordService;
import com.apishield.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordService passwordService;
    private final AccountService accountService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        UserResponse response = userService.getProfile(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        UserResponse response = userService.updateProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/change-password")
    public ResponseEntity<MessageResponse> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        passwordService.changePassword(userId, request);
        return ResponseEntity.ok(new MessageResponse("Password changed successfully. All active sessions have been invalidated. Please log in again."));
    }

    @PostMapping("/me/deactivate")
    public ResponseEntity<MessageResponse> deactivateAccount(
            Authentication authentication,
            @Valid @RequestBody DeactivateAccountRequest request
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        accountService.deactivateAccount(userId, request);
        return ResponseEntity.ok(new MessageResponse("Account has been deactivated successfully. All active sessions have been terminated."));
    }
}
