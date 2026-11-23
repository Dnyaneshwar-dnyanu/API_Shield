package com.apishield.notification.controller;

import com.apishield.common.constant.HeaderConstants;
import com.apishield.notification.dto.DirectNotificationRequest;
import com.apishield.notification.dto.NotificationResponse;
import com.apishield.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Value("${apishield.security.internal-token:apishield-internal-secret-token-key-32bytes-min!}")
    private String internalTokenSecret;

    @PostMapping("/send-direct")
    public ResponseEntity<NotificationResponse> sendDirectNotification(
            @RequestHeader(value = HeaderConstants.X_INTERNAL_TOKEN, required = false) String internalToken,
            @Valid @RequestBody DirectNotificationRequest request
    ) {
        if (internalToken == null || !internalToken.equals(internalTokenSecret)) {
            log.warn("Unauthorized attempt to access send-direct endpoint with invalid internal token");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing internal service token");
        }

        NotificationResponse response = notificationService.sendDirect(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @RequestHeader(value = HeaderConstants.X_USER_ID, required = false) String userId,
            @RequestParam(value = "unreadOnly", defaultValue = "false") boolean unreadOnly,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id header");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<NotificationResponse> result = notificationService.getUserNotifications(userId, unreadOnly, pageable);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable("id") UUID notificationId,
            @RequestHeader(value = HeaderConstants.X_USER_ID, required = false) String userId
    ) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id header");
        }

        NotificationResponse response = notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestHeader(value = HeaderConstants.X_USER_ID, required = false) String userId
    ) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id header");
        }

        long unreadCount = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
    }
}
