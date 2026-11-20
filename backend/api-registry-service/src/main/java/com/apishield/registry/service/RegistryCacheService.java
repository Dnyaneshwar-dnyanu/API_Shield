package com.apishield.registry.service;

import com.apishield.registry.dto.InternalApiResolutionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistryCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_KEY_PREFIX = "api:metadata:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    public void warmApiMetadata(InternalApiResolutionResponse metadata) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String key = CACHE_KEY_PREFIX + metadata.getSlug();
            String json = objectMapper.writeValueAsString(metadata);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL);
            log.info("Warmed Redis routing cache for key: {}", key);
        } catch (Exception e) {
            log.warn("Could not warm Redis metadata for slug {}: {}", metadata.getSlug(), e.getMessage());
        }
    }

    public void evictApiMetadata(String slug) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String key = CACHE_KEY_PREFIX + slug;
            redisTemplate.delete(key);
            log.info("Evicted Redis routing cache for key: {}", key);
        } catch (Exception e) {
            log.warn("Could not evict Redis metadata for slug {}: {}", slug, e.getMessage());
        }
    }
}
