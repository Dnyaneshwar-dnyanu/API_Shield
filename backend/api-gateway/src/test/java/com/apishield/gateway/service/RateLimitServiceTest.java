package com.apishield.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private RedisScript<Long> tokenBucketScript;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(redisTemplate, tokenBucketScript);
    }

    @Test
    @DisplayName("Should permit request when token bucket script returns 1")
    @SuppressWarnings("unchecked")
    void shouldPermitRequestWhenUnderLimit() {
        UUID keyId = UUID.randomUUID();
        when(redisTemplate.execute(eq(tokenBucketScript), any(List.class), any(List.class)))
                .thenReturn(Flux.just(1L));

        StepVerifier.create(rateLimitService.isAllowed(keyId, 10))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should reject request when token bucket script returns 0")
    @SuppressWarnings("unchecked")
    void shouldRejectRequestWhenOverLimit() {
        UUID keyId = UUID.randomUUID();
        when(redisTemplate.execute(eq(tokenBucketScript), any(List.class), any(List.class)))
                .thenReturn(Flux.just(0L));

        StepVerifier.create(rateLimitService.isAllowed(keyId, 10))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should fail open (return true) when Redis throws an exception")
    @SuppressWarnings("unchecked")
    void shouldFailOpenOnRedisError() {
        UUID keyId = UUID.randomUUID();
        when(redisTemplate.execute(eq(tokenBucketScript), any(List.class), any(List.class)))
                .thenReturn(Flux.error(new RuntimeException("Redis connection timed out")));

        StepVerifier.create(rateLimitService.isAllowed(keyId, 10))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return true immediately for null key or zero RPS")
    void shouldReturnTrueForInvalidParams() {
        StepVerifier.create(rateLimitService.isAllowed(null, 10))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(rateLimitService.isAllowed(UUID.randomUUID(), 0))
                .expectNext(true)
                .verifyComplete();
    }
}
