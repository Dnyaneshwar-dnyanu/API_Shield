package com.apishield.apikey.controller;

import com.apishield.apikey.dto.ApiKeyResponse;
import com.apishield.apikey.dto.CreateKeyRequest;
import com.apishield.apikey.dto.CreateKeyResponse;
import com.apishield.apikey.dto.RevokeKeyRequest;
import com.apishield.apikey.service.ApiKeyService;
import com.apishield.common.constant.HeaderConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    public ResponseEntity<CreateKeyResponse> generateKey(
            @Valid @RequestBody CreateKeyRequest request,
            @RequestHeader(value = HeaderConstants.X_USER_ID, required = false) String developerId) {
        log.info("REST: generate new API key for API: {}", request.getApiId());
        CreateKeyResponse response = apiKeyService.generateKey(request, developerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> listKeys(
            @RequestHeader(value = HeaderConstants.X_USER_ID, required = false) String developerId) {
        log.info("REST: list API keys for developer: {}", developerId);
        if (developerId == null || developerId.isBlank()) {
            throw new IllegalArgumentException("X-User-Id header is required to list API keys");
        }
        return ResponseEntity.ok(apiKeyService.listKeys(developerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiKeyResponse> getKeyById(
            @PathVariable UUID id,
            @RequestHeader(value = HeaderConstants.X_USER_ID, required = false) String developerId) {
        log.info("REST: get API key details for ID: {}", id);
        return ResponseEntity.ok(apiKeyService.getKeyById(id, developerId));
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<ApiKeyResponse> revokeKey(
            @PathVariable UUID id,
            @RequestBody(required = false) RevokeKeyRequest request,
            @RequestHeader(value = HeaderConstants.X_USER_ID, required = false) String developerId) {
        log.info("REST: revoke API key ID: {}", id);
        String reason = request != null ? request.getReason() : "Revoked by user";
        return ResponseEntity.ok(apiKeyService.revokeKey(id, developerId, reason));
    }

    @PostMapping("/{id}/roll")
    public ResponseEntity<CreateKeyResponse> rollKey(
            @PathVariable UUID id,
            @RequestHeader(value = HeaderConstants.X_USER_ID, required = false) String developerId) {
        log.info("REST: roll API key ID: {}", id);
        return ResponseEntity.ok(apiKeyService.rollKey(id, developerId));
    }
}
