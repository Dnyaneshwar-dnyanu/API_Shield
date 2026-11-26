package com.apishield.gateway.service;

import com.apishield.gateway.config.ApiShieldGatewayProperties;
import com.apishield.gateway.dto.ApiMetadata;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiResolutionService {

    private static final String CACHE_KEY_PREFIX = "api:metadata:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final WebClient webClient;
    private final ApiShieldGatewayProperties gatewayProperties;
    private final ObjectMapper objectMapper;

    public Mono<ApiMetadata> resolveApi(String slug) {
        if (slug == null || slug.isBlank()) {
            return Mono.empty();
        }

        String normalizedSlug = slug.trim().toLowerCase();
        String redisKey = CACHE_KEY_PREFIX + normalizedSlug;

        // 1. Try Redis cache first
        return redisTemplate.opsForValue().get(redisKey)
                .flatMap(json -> parseMetadata(json, normalizedSlug))
                .switchIfEmpty(Mono.defer(() -> fetchFromRegistryService(normalizedSlug, redisKey)));
    }

    private Mono<ApiMetadata> parseMetadata(String json, String slug) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String idStr = root.path("apiId").asText(null);
            if (idStr == null) {
                idStr = root.path("id").asText(null);
            }
            UUID apiId = idStr != null ? UUID.fromString(idStr) : null;
            String targetBackendUrl = root.path("targetBackendUrl").asText("");
            String status = root.path("status").asText("PUBLISHED");

            ApiMetadata metadata = ApiMetadata.builder()
                    .apiId(apiId)
                    .slug(slug)
                    .targetBackendUrl(targetBackendUrl)
                    .status(status)
                    .build();

            return Mono.just(metadata);
        } catch (Exception e) {
            log.warn("Failed to parse cached API metadata for slug {}: {}", slug, e.getMessage());
            return Mono.empty();
        }
    }

    private Mono<ApiMetadata> fetchFromRegistryService(String slug, String redisKey) {
        String url = gatewayProperties.getServices().getRegistryServiceUrl() + "/api/v1/internal/apis/resolve/" + slug;
        log.info("Cache miss for slug '{}'. Fetching from Registry Service: {}", slug, url);

        return webClient.get()
                .uri(url)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> {
                    log.warn("Registry service returned status {} for slug {}", clientResponse.statusCode(), slug);
                    return Mono.empty();
                })
                .bodyToMono(String.class)
                .flatMap(json -> parseMetadata(json, slug)
                        .flatMap(metadata -> {
                            if ("PUBLISHED".equalsIgnoreCase(metadata.getStatus())) {
                                return redisTemplate.opsForValue().set(redisKey, json, CACHE_TTL)
                                        .thenReturn(metadata)
                                        .onErrorReturn(metadata);
                            }
                            return Mono.just(metadata);
                        }))
                .onErrorResume(e -> {
                    log.warn("Error calling registry service for slug {}: {}", slug, e.getMessage());
                    return Mono.empty();
                });
    }
}
