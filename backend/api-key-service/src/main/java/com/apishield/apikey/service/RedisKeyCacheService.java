package com.apishield.apikey.service;

import com.apishield.apikey.entity.ApiKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisKeyCacheService {

    private static final String KEY_PREFIX = "apikey:auth:";
    private static final Duration DEFAULT_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Data
    @Builder
    public static class CachedKeyData {
        private UUID keyId;
        private String developerId;
        private String subscriptionId;
        private String apiId;
        private int rateLimitRps;
        private int quotaMonthly;
        private String status;
    }

    public void cacheKey(ApiKey key) {
        try {
            if (redisTemplate == null) {
                return;
            }
            CachedKeyData data = CachedKeyData.builder()
                    .keyId(key.getId())
                    .developerId(key.getDeveloperId())
                    .subscriptionId(key.getSubscriptionId())
                    .apiId(key.getApiId())
                    .rateLimitRps(key.getRateLimitRps())
                    .quotaMonthly(key.getQuotaMonthly())
                    .status(key.getStatus().name())
                    .build();

            String json = objectMapper.writeValueAsString(data);
            String redisKey = KEY_PREFIX + key.getKeyHash();
            redisTemplate.opsForValue().set(redisKey, json, DEFAULT_TTL);
            log.info("Cached API key in Redis: {}", redisKey);
        } catch (Exception e) {
            log.warn("Failed to cache API key in Redis: {}", e.getMessage());
        }
    }

    public void evictKey(String keyHash) {
        try {
            if (redisTemplate == null || keyHash == null) {
                return;
            }
            String redisKey = KEY_PREFIX + keyHash;
            Boolean deleted = redisTemplate.delete(redisKey);
            log.info("Evicted API key from Redis: {} (result: {})", redisKey, deleted);
        } catch (Exception e) {
            log.warn("Failed to evict API key from Redis: {}", e.getMessage());
        }
    }

    public Optional<CachedKeyData> getCachedKey(String keyHash) {
        try {
            if (redisTemplate == null || keyHash == null) {
                return Optional.empty();
            }
            String redisKey = KEY_PREFIX + keyHash;
            String json = redisTemplate.opsForValue().get(redisKey);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            CachedKeyData data = objectMapper.readValue(json, CachedKeyData.class);
            return Optional.of(data);
        } catch (Exception e) {
            log.warn("Failed to retrieve cached API key from Redis: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
