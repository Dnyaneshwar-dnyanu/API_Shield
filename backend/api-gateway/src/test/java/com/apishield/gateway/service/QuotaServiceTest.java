package com.apishield.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuotaServiceTest {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    private QuotaService quotaService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        quotaService = new QuotaService(redisTemplate);
    }

    @Test
    @DisplayName("Should set TTL and allow request on first call of month (count == 1)")
    void shouldSetTtlAndAllowOnFirstCall() {
        UUID keyId = UUID.randomUUID();
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));

        StepVerifier.create(quotaService.checkAndIncrementQuota(keyId, 1000))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should allow request when within monthly quota")
    void shouldAllowWhenWithinQuota() {
        UUID keyId = UUID.randomUUID();
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(500L));

        StepVerifier.create(quotaService.checkAndIncrementQuota(keyId, 1000))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should reject request when monthly quota is exceeded")
    void shouldRejectWhenQuotaExceeded() {
        UUID keyId = UUID.randomUUID();
        when(valueOperations.increment(anyString())).thenReturn(Mono.just(1001L));

        StepVerifier.create(quotaService.checkAndIncrementQuota(keyId, 1000))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should fail open (return true) when Redis throws an exception")
    void shouldFailOpenOnRedisError() {
        UUID keyId = UUID.randomUUID();
        when(valueOperations.increment(anyString()))
                .thenReturn(Mono.error(new RuntimeException("Redis cluster unreachable")));

        StepVerifier.create(quotaService.checkAndIncrementQuota(keyId, 1000))
                .expectNext(true)
                .verifyComplete();
    }
}
