package com.apishield.registry.controller;

import com.apishield.registry.dto.InternalApiResolutionResponse;
import com.apishield.registry.dto.PlanResponse;
import com.apishield.registry.service.InternalResolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalApiController {

    private final InternalResolutionService internalService;

    @GetMapping("/apis/resolve/{slug}")
    public ResponseEntity<InternalApiResolutionResponse> resolveApiBySlug(@PathVariable String slug) {
        InternalApiResolutionResponse response = internalService.resolveApiBySlug(slug);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/plans/{planId}")
    public ResponseEntity<PlanResponse> getPlanById(@PathVariable UUID planId) {
        PlanResponse response = internalService.getPlanById(planId);
        return ResponseEntity.ok(response);
    }
}
