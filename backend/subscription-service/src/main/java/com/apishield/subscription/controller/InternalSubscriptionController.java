package com.apishield.subscription.controller;

import com.apishield.subscription.dto.SubscriptionResponse;
import com.apishield.subscription.dto.SubscriptionValidationResponse;
import com.apishield.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/subscriptions")
@RequiredArgsConstructor
public class InternalSubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/validate")
    public ResponseEntity<SubscriptionValidationResponse> validateSubscription(
            @RequestParam String developerId,
            @RequestParam String apiId) {
        SubscriptionValidationResponse response = subscriptionService.validateSubscription(developerId, apiId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> getSubscriptionById(@PathVariable UUID id) {
        SubscriptionResponse response = subscriptionService.getInternalSubscriptionById(id);
        return ResponseEntity.ok(response);
    }
}
