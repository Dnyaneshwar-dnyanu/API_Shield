package com.apishield.gateway.service;

import com.apishield.gateway.config.GatewayProperties;
import com.apishield.gateway.dto.ApiMetadata;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiResolutionServiceTest {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @Mock
    private WebClient webClient;

    private GatewayProperties gatewayProperties;
    private ObjectMapper objectMapper;
    private ApiResolutionService apiResolutionService;

    @BeforeEach
    void setUp() {
        gatewayProperties = new GatewayProperties();
        objectMapper = new ObjectMapper();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        apiResolutionService = new ApiResolutionService(
                redisTemplate,
                webClient,
                gatewayProperties,
                objectMapper
        );
    }

    @Test
    @DisplayName("Should return ApiMetadata from Redis cache when present")
    void shouldReturnFromCacheWhenPresent() {
        UUID apiId = UUID.randomUUID();
        String json = "{\"apiId\":\"" + apiId + "\",\"slug\":\"weather\",\"targetBackendUrl\":\"https://backend.com\",\"status\":\"PUBLISHED\"}";

        when(valueOperations.get("api:metadata:weather")).thenReturn(Mono.just(json));

        StepVerifier.create(apiResolutionService.resolveApi("weather"))
                .assertNext(meta -> {
                    assertThat(meta.getApiId()).isEqualTo(apiId);
                    assertThat(meta.getSlug()).isEqualTo("weather");
                    assertThat(meta.getTargetBackendUrl()).isEqualTo("https://backend.com");
                    assertThat(meta.getStatus()).isEqualTo("PUBLISHED");
                })
                .verifyComplete();

        verifyNoInteractions(webClient);
    }

    @Test
    @DisplayName("Should return Mono.empty() for blank slug")
    void shouldReturnEmptyForBlankSlug() {
        StepVerifier.create(apiResolutionService.resolveApi("   "))
                .verifyComplete();
    }
}
