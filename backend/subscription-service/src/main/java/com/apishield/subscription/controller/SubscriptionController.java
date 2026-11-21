package com.apishield.subscription.controller;

import com.apishield.common.constant.HeaderConstants;
import com.apishield.subscription.dto.CreateSubscriptionRequest;
import com.apishield.subscription.dto.SubscriptionResponse;
import com.apishield.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @Valid @RequestBody CreateSubscriptionRequest request,
            @RequestHeader(value = HeaderConstants.X_USER_ID, defaultValue = "developer-default-id") String developerId) {
        SubscriptionResponse response = subscriptionService.createSubscription(request, developerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<Page<SubscriptionResponse>> getMySubscriptions(
            @RequestHeader(value = HeaderConstants.X_USER_ID, defaultValue = "developer-default-id") String developerId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<SubscriptionResponse> response = subscriptionService.getMySubscriptions(developerId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> getSubscription(
            @PathVariable UUID id,
            @RequestHeader(value = HeaderConstants.X_USER_ID, defaultValue = "developer-default-id") String developerId) {
        SubscriptionResponse response = subscriptionService.getSubscriptionById(id, developerId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(
            @PathVariable UUID id,
            @RequestHeader(value = HeaderConstants.X_USER_ID, defaultValue = "developer-default-id") String developerId) {
        SubscriptionResponse response = subscriptionService.cancelSubscription(id, developerId);
        return ResponseEntity.ok(response);
    }
}
