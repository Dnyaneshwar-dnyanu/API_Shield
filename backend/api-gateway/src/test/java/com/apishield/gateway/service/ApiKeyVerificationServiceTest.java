package com.apishield.gateway.service;

import com.apishield.common.util.CryptoUtils;
import com.apishield.gateway.config.ApiShieldGatewayProperties;
import com.apishield.gateway.dto.ApiKeyAuthData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyVerificationServiceTest {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @Mock
    private WebClient webClient;

    private ApiShieldGatewayProperties gatewayProperties;
    private ObjectMapper objectMapper;
    private ApiKeyVerificationService keyVerificationService;

    @BeforeEach
    void setUp() {
        gatewayProperties = new ApiShieldGatewayProperties();
        objectMapper = new ObjectMapper();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        keyVerificationService = new ApiKeyVerificationService(
                redisTemplate,
                webClient,
                gatewayProperties,
                objectMapper
        );
    }

    @Test
    @DisplayName("Should return ApiKeyAuthData from Redis cache when present")
    void shouldReturnFromCacheWhenPresent() {
        String rawKey = "ash_live_test_123456";
        String keyHash = CryptoUtils.sha256Hex(rawKey);
        UUID keyId = UUID.randomUUID();

        String json = "{\"keyId\":\"" + keyId + "\",\"developerId\":\"dev-99\",\"apiId\":\"api-11\",\"rateLimitRps\":20,\"quotaMonthly\":50000,\"status\":\"ACTIVE\"}";

        when(valueOperations.get("apikey:auth:" + keyHash)).thenReturn(Mono.just(json));

        StepVerifier.create(keyVerificationService.verifyKey(rawKey))
                .assertNext(auth -> {
                    assertThat(auth.getKeyId()).isEqualTo(keyId);
                    assertThat(auth.getDeveloperId()).isEqualTo("dev-99");
                    assertThat(auth.getApiId()).isEqualTo("api-11");
                    assertThat(auth.getRateLimitRps()).isEqualTo(20);
                    assertThat(auth.getQuotaMonthly()).isEqualTo(50000);
                    assertThat(auth.getStatus()).isEqualTo("ACTIVE");
                })
                .verifyComplete();

        verifyNoInteractions(webClient);
    }

    @Test
    @DisplayName("Should return Mono.empty() for blank API key")
    void shouldReturnEmptyForBlankKey() {
        StepVerifier.create(keyVerificationService.verifyKey("   "))
                .verifyComplete();
    }
}
