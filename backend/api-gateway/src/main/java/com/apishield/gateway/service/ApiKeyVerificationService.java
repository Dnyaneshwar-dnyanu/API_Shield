package com.apishield.gateway.service;

import com.apishield.common.util.CryptoUtils;
import com.apishield.gateway.config.ApiShieldGatewayProperties;
import com.apishield.gateway.dto.ApiKeyAuthData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyVerificationService {

    private static final String CACHE_KEY_PREFIX = "apikey:auth:";
    private static final Duration CACHE_TTL = Duration.ofDays(7);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final WebClient webClient;
    private final ApiShieldGatewayProperties gatewayProperties;
    private final ObjectMapper objectMapper;

    public Mono<ApiKeyAuthData> verifyKey(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            return Mono.empty();
        }

        String keyHash = CryptoUtils.sha256Hex(rawApiKey.trim());
        String redisKey = CACHE_KEY_PREFIX + keyHash;

        // 1. Check Redis cache first
        return redisTemplate.opsForValue().get(redisKey)
                .flatMap(this::parseAuthData)
                .switchIfEmpty(Mono.defer(() -> fetchFromApiKeyService(rawApiKey, keyHash, redisKey)));
    }

    private Mono<ApiKeyAuthData> parseAuthData(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String idStr = root.path("keyId").asText(null);
            if (idStr == null) {
                idStr = root.path("id").asText(null);
            }
            UUID keyId = idStr != null ? UUID.fromString(idStr) : null;
            String developerId = root.path("developerId").asText(null);
            String subscriptionId = root.path("subscriptionId").asText(null);
            String apiId = root.path("apiId").asText(null);
            int rateLimitRps = root.path("rateLimitRps").asInt(10);
            int quotaMonthly = root.path("quotaMonthly").asInt(10000);
            String status = root.path("status").asText("ACTIVE");

            ApiKeyAuthData authData = ApiKeyAuthData.builder()
                    .keyId(keyId)
                    .developerId(developerId)
                    .subscriptionId(subscriptionId)
                    .apiId(apiId)
                    .rateLimitRps(rateLimitRps)
                    .quotaMonthly(quotaMonthly)
                    .status(status)
                    .build();

            return Mono.just(authData);
        } catch (Exception e) {
            log.warn("Failed to parse cached API key data: {}", e.getMessage());
            return Mono.empty();
        }
    }

    private Mono<ApiKeyAuthData> fetchFromApiKeyService(String rawApiKey, String keyHash, String redisKey) {
        String url = gatewayProperties.getServices().getApiKeyServiceUrl() + "/api/v1/internal/keys/verify";
        log.info("Cache miss for key hash '{}'. Verifying with Key Service: {}", keyHash, url);

        Map<String, String> requestBody = Map.of(
                "apiKey", rawApiKey,
                "keyHash", keyHash
        );

        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> {
                    log.warn("Key service verification failed with status: {}", clientResponse.statusCode());
                    return Mono.empty();
                })
                .bodyToMono(String.class)
                .flatMap(json -> {
                    try {
                        JsonNode root = objectMapper.readTree(json);
                        boolean valid = root.path("valid").asBoolean(false);
                        if (!valid) {
                            return Mono.empty();
                        }
                        return parseAuthData(json).flatMap(authData -> {
                            if ("ACTIVE".equalsIgnoreCase(authData.getStatus())) {
                                try {
                                    String cacheJson = objectMapper.writeValueAsString(authData);
                                    return redisTemplate.opsForValue().set(redisKey, cacheJson, CACHE_TTL)
                                            .thenReturn(authData)
                                            .onErrorReturn(authData);
                                } catch (Exception e) {
                                    return Mono.just(authData);
                                }
                            }
                            return Mono.just(authData);
                        });
                    } catch (Exception e) {
                        log.warn("Failed to parse key service response: {}", e.getMessage());
                        return Mono.empty();
                    }
                })
                .onErrorResume(e -> {
                    log.warn("Error calling key service: {}", e.getMessage());
                    return Mono.empty();
                });
    }
}
