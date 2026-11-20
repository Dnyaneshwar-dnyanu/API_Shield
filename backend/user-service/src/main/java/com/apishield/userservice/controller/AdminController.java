package com.apishield.userservice.controller;

import com.apishield.userservice.dto.request.AdminUpdateUserRequest;
import com.apishield.userservice.dto.request.SuspendUserRequest;
import com.apishield.userservice.dto.request.UpdateUserRoleRequest;
import com.apishield.userservice.dto.response.AuditLogResponse;
import com.apishield.userservice.dto.response.MessageResponse;
import com.apishield.userservice.dto.response.PageResponse;
import com.apishield.userservice.dto.response.UserResponse;
import com.apishield.userservice.enums.AccountStatus;
import com.apishield.userservice.enums.Role;
import com.apishield.userservice.service.AdminUserService;
import com.apishield.userservice.service.AuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminUserService adminUserService;
    private final AuditLogService auditLogService;

    @GetMapping("/users")
    public ResponseEntity<PageResponse<UserResponse>> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        PageResponse<UserResponse> response = adminUserService.listUsers(search, role, status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        UserResponse response = adminUserService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/users/{userId}")
    public ResponseEntity<UserResponse> updateUserDetails(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUpdateUserRequest request,
            Authentication authentication
    ) {
        UUID adminId = UUID.fromString(authentication.getName());
        UserResponse response = adminUserService.updateUserDetails(userId, request, adminId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Authentication authentication
    ) {
        UUID adminId = UUID.fromString(authentication.getName());
        UserResponse response = adminUserService.updateUserRole(userId, request, adminId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{userId}/activate")
    public ResponseEntity<UserResponse> activateUser(
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        UUID adminId = UUID.fromString(authentication.getName());
        UserResponse response = adminUserService.activateUser(userId, adminId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{userId}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        UUID adminId = UUID.fromString(authentication.getName());
        UserResponse response = adminUserService.deactivateUser(userId, adminId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{userId}/suspend")
    public ResponseEntity<UserResponse> suspendUser(
            @PathVariable UUID userId,
            @Valid @RequestBody(required = false) SuspendUserRequest request,
            Authentication authentication
    ) {
        UUID adminId = UUID.fromString(authentication.getName());
        UserResponse response = adminUserService.suspendUser(userId, request, adminId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{userId}/reactivate")
    public ResponseEntity<UserResponse> reactivateUser(
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        UUID adminId = UUID.fromString(authentication.getName());
        UserResponse response = adminUserService.reactivateUser(userId, adminId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<MessageResponse> deleteUser(
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        UUID adminId = UUID.fromString(authentication.getName());
        adminUserService.deleteUser(userId, adminId);
        return ResponseEntity.ok(new MessageResponse("User account has been soft-deleted successfully."));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<PageResponse<AuditLogResponse>> listAuditLogs(
            @RequestParam(required = false) UUID targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<AuditLogResponse> response = auditLogService.listAuditLogs(targetId, pageable);
        return ResponseEntity.ok(response);
    }
}
