package com.apishield.apikey.service.impl;

import com.apishield.apikey.client.SubscriptionValidationClient;
import com.apishield.apikey.crypto.KeyGenerator;
import com.apishield.apikey.dto.*;
import com.apishield.apikey.entity.ApiKey;
import com.apishield.apikey.exception.ConflictException;
import com.apishield.apikey.exception.ResourceNotFoundException;
import com.apishield.apikey.model.KeyStatus;
import com.apishield.apikey.repository.ApiKeyRepository;
import com.apishield.apikey.service.ApiKeyPublisher;
import com.apishield.apikey.service.ApiKeyService;
import com.apishield.apikey.service.RedisKeyCacheService;
import com.apishield.common.util.CryptoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final KeyGenerator keyGenerator;
    private final RedisKeyCacheService redisKeyCacheService;
    private final SubscriptionValidationClient subscriptionValidationClient;
    private final ApiKeyPublisher apiKeyPublisher;

    @Override
    @Transactional
    public CreateKeyResponse generateKey(CreateKeyRequest request, String developerId) {
        log.info("Generating new API key for developer: {}, API: {}, subscription: {}",
                developerId, request.getApiId(), request.getSubscriptionId());

        if (developerId == null || developerId.isBlank()) {
            throw new IllegalArgumentException("Developer ID is required to generate an API key");
        }

        SubscriptionValidationDto subDto = subscriptionValidationClient
                .validateSubscription(developerId, request.getApiId())
                .orElseThrow(() -> new ResourceNotFoundException("Active subscription could not be verified for API: " + request.getApiId()));

        if (!subDto.isValid() || !"ACTIVE".equalsIgnoreCase(subDto.getStatus())) {
            throw new ConflictException("Developer does not possess an ACTIVE subscription for this API (current: " + subDto.getStatus() + ")");
        }

        KeyGenerator.GeneratedKey generated = keyGenerator.generate();

        ApiKey apiKey = ApiKey.builder()
                .name(request.getName().trim())
                .developerId(developerId)
                .subscriptionId(request.getSubscriptionId())
                .apiId(request.getApiId())
                .keyPrefix(generated.keyPrefix())
                .keyHash(generated.keyHash())
                .status(KeyStatus.ACTIVE)
                .rateLimitRps(subDto.getRateLimitRps() > 0 ? subDto.getRateLimitRps() : 10)
                .quotaMonthly(subDto.getQuotaMonthly() > 0 ? subDto.getQuotaMonthly() : 10000)
                .build();

        ApiKey saved = apiKeyRepository.save(apiKey);
        log.info("Persisted API key: {} with prefix: {}", saved.getId(), saved.getKeyPrefix());

        redisKeyCacheService.cacheKey(saved);

        return CreateKeyResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .apiKey(generated.rawKey()) // Plaintext returned exactly once!
                .keyPrefix(saved.getKeyPrefix())
                .apiId(saved.getApiId())
                .subscriptionId(saved.getSubscriptionId())
                .status(saved.getStatus())
                .rateLimitRps(saved.getRateLimitRps())
                .quotaMonthly(saved.getQuotaMonthly())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiKeyResponse> listKeys(String developerId) {
        return apiKeyRepository.findByDeveloperIdOrderByCreatedAtDesc(developerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ApiKeyResponse getKeyById(UUID id, String developerId) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found: " + id));

        if (developerId != null && !developerId.isBlank() && !developerId.equals(apiKey.getDeveloperId())) {
            throw new IllegalArgumentException("API key does not belong to the current developer");
        }

        return mapToResponse(apiKey);
    }

    @Override
    @Transactional
    public ApiKeyResponse revokeKey(UUID id, String developerId, String reason) {
        log.info("Revoking API key: {} for developer: {}", id, developerId);

        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found: " + id));

        if (developerId != null && !developerId.isBlank() && !developerId.equals(apiKey.getDeveloperId())) {
            throw new IllegalArgumentException("API key does not belong to the current developer");
        }

        if (apiKey.getStatus() == KeyStatus.REVOKED) {
            log.info("API key {} is already REVOKED", id);
            return mapToResponse(apiKey);
        }

        apiKey.setStatus(KeyStatus.REVOKED);
        apiKey.setUpdatedAt(Instant.now());
        ApiKey saved = apiKeyRepository.save(apiKey);

        redisKeyCacheService.evictKey(saved.getKeyHash());
        apiKeyPublisher.publishKeyRevoked(saved, reason);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CreateKeyResponse rollKey(UUID id, String developerId) {
        log.info("Rolling API key: {} for developer: {}", id, developerId);

        ApiKey oldKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found: " + id));

        if (developerId != null && !developerId.isBlank() && !developerId.equals(oldKey.getDeveloperId())) {
            throw new IllegalArgumentException("API key does not belong to the current developer");
        }

        // 1. Revoke the old key
        if (oldKey.getStatus() != KeyStatus.REVOKED) {
            oldKey.setStatus(KeyStatus.REVOKED);
            oldKey.setUpdatedAt(Instant.now());
            apiKeyRepository.save(oldKey);
            redisKeyCacheService.evictKey(oldKey.getKeyHash());
            apiKeyPublisher.publishKeyRevoked(oldKey, "Key rolled / rotated");
        }

        // 2. Generate new key
        KeyGenerator.GeneratedKey generated = keyGenerator.generate();
        ApiKey newKey = ApiKey.builder()
                .name(oldKey.getName())
                .developerId(oldKey.getDeveloperId())
                .subscriptionId(oldKey.getSubscriptionId())
                .apiId(oldKey.getApiId())
                .keyPrefix(generated.keyPrefix())
                .keyHash(generated.keyHash())
                .status(KeyStatus.ACTIVE)
                .rateLimitRps(oldKey.getRateLimitRps())
                .quotaMonthly(oldKey.getQuotaMonthly())
                .build();

        ApiKey savedNew = apiKeyRepository.save(newKey);
        redisKeyCacheService.cacheKey(savedNew);

        return CreateKeyResponse.builder()
                .id(savedNew.getId())
                .name(savedNew.getName())
                .apiKey(generated.rawKey())
                .keyPrefix(savedNew.getKeyPrefix())
                .apiId(savedNew.getApiId())
                .subscriptionId(savedNew.getSubscriptionId())
                .status(savedNew.getStatus())
                .rateLimitRps(savedNew.getRateLimitRps())
                .quotaMonthly(savedNew.getQuotaMonthly())
                .createdAt(savedNew.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void revokeKeysBySubscriptionId(String subscriptionId, String reason) {
        log.info("Revoking all active API keys for subscription: {}", subscriptionId);
        List<ApiKey> activeKeys = apiKeyRepository.findBySubscriptionIdAndStatus(subscriptionId, KeyStatus.ACTIVE);
        for (ApiKey key : activeKeys) {
            key.setStatus(KeyStatus.REVOKED);
            key.setUpdatedAt(Instant.now());
            apiKeyRepository.save(key);
            redisKeyCacheService.evictKey(key.getKeyHash());
            apiKeyPublisher.publishKeyRevoked(key, reason);
            log.info("Revoked API key {} due to subscription termination", key.getId());
        }
    }

    @Override
    @Transactional
    public KeyValidationResponse verifyKey(VerifyKeyRequest request) {
        String keyHash = request.getKeyHash();
        if (keyHash == null || keyHash.isBlank()) {
            if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
                keyHash = CryptoUtils.sha256Hex(request.getApiKey().trim());
            } else {
                return KeyValidationResponse.builder()
                        .valid(false)
                        .message("Either apiKey or keyHash must be provided")
                        .build();
            }
        }

        // 1. Check Redis cache first
        Optional<RedisKeyCacheService.CachedKeyData> cached = redisKeyCacheService.getCachedKey(keyHash);
        if (cached.isPresent()) {
            RedisKeyCacheService.CachedKeyData data = cached.get();
            if ("ACTIVE".equalsIgnoreCase(data.getStatus())) {
                return KeyValidationResponse.builder()
                        .valid(true)
                        .keyId(data.getKeyId())
                        .developerId(data.getDeveloperId())
                        .subscriptionId(data.getSubscriptionId())
                        .apiId(data.getApiId())
                        .rateLimitRps(data.getRateLimitRps())
                        .quotaMonthly(data.getQuotaMonthly())
                        .status(data.getStatus())
                        .message("Valid API key (from cache)")
                        .build();
            } else {
                return KeyValidationResponse.builder()
                        .valid(false)
                        .status(data.getStatus())
                        .message("API key is not active")
                        .build();
            }
        }

        // 2. Fallback to DB
        Optional<ApiKey> dbKey = apiKeyRepository.findByKeyHash(keyHash);
        if (dbKey.isEmpty()) {
            return KeyValidationResponse.builder()
                    .valid(false)
                    .message("API key not found")
                    .build();
        }

        ApiKey key = dbKey.get();
        if (key.getStatus() != KeyStatus.ACTIVE) {
            return KeyValidationResponse.builder()
                    .valid(false)
                    .keyId(key.getId())
                    .status(key.getStatus().name())
                    .message("API key is " + key.getStatus())
                    .build();
        }

        // Update last used timestamp and warm Redis
        key.setLastUsedAt(Instant.now());
        apiKeyRepository.save(key);
        redisKeyCacheService.cacheKey(key);

        return KeyValidationResponse.builder()
                .valid(true)
                .keyId(key.getId())
                .developerId(key.getDeveloperId())
                .subscriptionId(key.getSubscriptionId())
                .apiId(key.getApiId())
                .rateLimitRps(key.getRateLimitRps())
                .quotaMonthly(key.getQuotaMonthly())
                .status(key.getStatus().name())
                .message("Valid API key")
                .build();
    }

    private ApiKeyResponse mapToResponse(ApiKey key) {
        return ApiKeyResponse.builder()
                .id(key.getId())
                .name(key.getName())
                .keyPrefix(key.getKeyPrefix())
                .apiId(key.getApiId())
                .subscriptionId(key.getSubscriptionId())
                .status(key.getStatus())
                .rateLimitRps(key.getRateLimitRps())
                .quotaMonthly(key.getQuotaMonthly())
                .expiresAt(key.getExpiresAt())
                .lastUsedAt(key.getLastUsedAt())
                .createdAt(key.getCreatedAt())
                .updatedAt(key.getUpdatedAt())
                .build();
    }
}
