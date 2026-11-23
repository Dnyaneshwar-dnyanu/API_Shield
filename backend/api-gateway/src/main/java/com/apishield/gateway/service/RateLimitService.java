package com.apishield.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RedisScript<Long> tokenBucketScript;

    /**
     * Executes the token bucket Lua script atomically.
     *
     * @param keyId Unique API Key ID
     * @param rateLimitRps Allowed requests per second
     * @return Mono<Boolean> true if request is permitted; false if rate limited
     */
    public Mono<Boolean> isAllowed(UUID keyId, int rateLimitRps) {
        if (keyId == null || rateLimitRps <= 0) {
            return Mono.just(true);
        }

        long epochSecond = System.currentTimeMillis() / 1000;
        String bucketKey = "ratelimit:" + keyId + ":" + epochSecond;

        List<String> keys = List.of(bucketKey);
        String limitArg = String.valueOf(rateLimitRps);
        String ttlArg = "2"; // 2 seconds TTL window

        return redisTemplate.execute(tokenBucketScript, keys, List.of(limitArg, ttlArg))
                .next()
                .map(result -> result != null && result == 1L)
                .onErrorResume(e -> {
                    log.warn("Redis rate limiter check failed for keyId {}: {}. Failing open.", keyId, e.getMessage());
                    return Mono.just(true);
                });
    }
}
