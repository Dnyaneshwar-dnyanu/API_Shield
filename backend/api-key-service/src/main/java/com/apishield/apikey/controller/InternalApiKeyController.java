package com.apishield.apikey.controller;

import com.apishield.apikey.dto.KeyValidationResponse;
import com.apishield.apikey.dto.VerifyKeyRequest;
import com.apishield.apikey.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal/keys")
@RequiredArgsConstructor
public class InternalApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping("/verify")
    public ResponseEntity<KeyValidationResponse> verifyKey(@RequestBody VerifyKeyRequest request) {
        log.info("REST: internal Gateway fallback key verification");
        KeyValidationResponse response = apiKeyService.verifyKey(request);
        return ResponseEntity.ok(response);
    }
}
