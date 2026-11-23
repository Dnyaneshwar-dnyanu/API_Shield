package com.apishield.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final Duration QUOTA_TTL = Duration.ofDays(35);

    private final ReactiveStringRedisTemplate redisTemplate;

    /**
     * Atomically increments and validates monthly quota consumption.
     *
     * @param keyId Unique API Key ID
     * @param quotaMonthly Total allowed requests per calendar month
     * @return Mono<Boolean> true if within monthly quota; false if exceeded
     */
    public Mono<Boolean> checkAndIncrementQuota(UUID keyId, int quotaMonthly) {
        if (keyId == null || quotaMonthly <= 0) {
            return Mono.just(true);
        }

        String yearMonth = YearMonth.now().format(MONTH_FORMATTER);
        String quotaKey = "quota:" + keyId + ":" + yearMonth;

        return redisTemplate.opsForValue().increment(quotaKey)
                .flatMap(currentCount -> {
                    if (currentCount == 1L) {
                        return redisTemplate.expire(quotaKey, QUOTA_TTL)
                                .thenReturn(currentCount <= quotaMonthly);
                    }
                    return Mono.just(currentCount <= quotaMonthly);
                })
                .onErrorResume(e -> {
                    log.warn("Redis monthly quota check failed for keyId {}: {}. Failing open.", keyId, e.getMessage());
                    return Mono.just(true);
                });
    }
}
