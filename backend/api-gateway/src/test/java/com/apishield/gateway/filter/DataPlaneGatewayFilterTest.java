package com.apishield.gateway.filter;

import com.apishield.common.constant.HeaderConstants;
import com.apishield.gateway.config.GatewayProperties;
import com.apishield.gateway.dto.ApiKeyAuthData;
import com.apishield.gateway.dto.ApiMetadata;
import com.apishield.gateway.service.ApiKeyVerificationService;
import com.apishield.gateway.service.ApiResolutionService;
import com.apishield.gateway.service.GatewayTelemetryPublisher;
import com.apishield.gateway.service.QuotaService;
import com.apishield.gateway.service.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataPlaneGatewayFilterTest {

    @Mock
    private ApiResolutionService apiResolutionService;

    @Mock
    private ApiKeyVerificationService apiKeyVerificationService;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private QuotaService quotaService;

    @Mock
    private GatewayTelemetryPublisher telemetryPublisher;

    private DataPlaneGatewayFilterFactory filterFactory;
    private GatewayProperties gatewayProperties;
    private ObjectMapper objectMapper;
    private GatewayFilter filter;

    private final UUID apiId = UUID.randomUUID();
    private final UUID keyId = UUID.randomUUID();
    private final String developerId = "dev-123";
    private final String rawApiKey = "ash_live_testprefix_secret123456789";

    @BeforeEach
    void setUp() {
        gatewayProperties = new GatewayProperties();
        gatewayProperties.setInternalSecret("test-internal-secret-token-key-32bytes-min!");

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        filterFactory = new DataPlaneGatewayFilterFactory(
                apiResolutionService,
                apiKeyVerificationService,
                rateLimitService,
                quotaService,
                telemetryPublisher,
                gatewayProperties,
                objectMapper
        );

        filter = filterFactory.apply(new DataPlaneGatewayFilterFactory.Config());
    }

    @Test
    @DisplayName("Phase 2: Should return 404 Not Found when API slug cannot be resolved")
    void shouldReturn404WhenApiSlugNotFound() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/gateway/unknown-api/v1/test")
                .header(HeaderConstants.X_API_KEY, rawApiKey)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(apiResolutionService.resolveApi("unknown-api")).thenReturn(Mono.empty());

        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(telemetryPublisher).publishTelemetry(any());
    }

    @Test
    @DisplayName("Phase 2: Should return 404 when API is not in PUBLISHED status")
    void shouldReturn404WhenApiNotPublished() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/gateway/weather/v1/forecast")
                .header(HeaderConstants.X_API_KEY, rawApiKey)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ApiMetadata meta = ApiMetadata.builder()
                .apiId(apiId)
                .slug("weather")
                .targetBackendUrl("http://weather-service:8080")
                .status("DRAFT")
                .build();
        when(apiResolutionService.resolveApi("weather")).thenReturn(Mono.just(meta));

        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Phase 3: Should return 401 Unauthorized when X-API-Key header is missing")
    void shouldReturn401WhenApiKeyMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/gateway/weather/v1/forecast")
                .build(); // No X-API-Key
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ApiMetadata meta = ApiMetadata.builder()
                .apiId(apiId)
                .slug("weather")
                .targetBackendUrl("http://weather-service:8080")
                .status("PUBLISHED")
                .build();
        when(apiResolutionService.resolveApi("weather")).thenReturn(Mono.just(meta));

        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Phase 3: Should return 401 Unauthorized when API key is unrecognized or invalid")
    void shouldReturn401WhenApiKeyInvalid() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/gateway/weather/v1/forecast")
                .header(HeaderConstants.X_API_KEY, "invalid_key")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ApiMetadata meta = ApiMetadata.builder()
                .apiId(apiId)
                .slug("weather")
                .targetBackendUrl("http://weather-service:8080")
                .status("PUBLISHED")
                .build();
        when(apiResolutionService.resolveApi("weather")).thenReturn(Mono.just(meta));
        when(apiKeyVerificationService.verifyKey("invalid_key")).thenReturn(Mono.empty());

        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Phase 4: Should return 403 Forbidden when API key is not ACTIVE")
    void shouldReturn403WhenApiKeyRevoked() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/gateway/weather/v1/forecast")
                .header(HeaderConstants.X_API_KEY, rawApiKey)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ApiMetadata meta = ApiMetadata.builder()
                .apiId(apiId)
                .slug("weather")
                .targetBackendUrl("http://weather-service:8080")
                .status("PUBLISHED")
                .build();
        ApiKeyAuthData auth = ApiKeyAuthData.builder()
                .keyId(keyId)
                .apiId(apiId.toString())
                .status("REVOKED")
                .build();

        when(apiResolutionService.resolveApi("weather")).thenReturn(Mono.just(meta));
        when(apiKeyVerificationService.verifyKey(rawApiKey)).thenReturn(Mono.just(auth));

        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Phase 4: Should return 403 Forbidden when API key is scoped to a different API")
    void shouldReturn403WhenApiKeyScopedToDifferentApi() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/gateway/weather/v1/forecast")
                .header(HeaderConstants.X_API_KEY, rawApiKey)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ApiMetadata meta = ApiMetadata.builder()
                .apiId(apiId)
                .slug("weather")
                .targetBackendUrl("http://weather-service:8080")
                .status("PUBLISHED")
                .build();
        ApiKeyAuthData auth = ApiKeyAuthData.builder()
                .keyId(keyId)
                .apiId(UUID.randomUUID().toString()) // Different API!
                .status("ACTIVE")
                .build();

        when(apiResolutionService.resolveApi("weather")).thenReturn(Mono.just(meta));
        when(apiKeyVerificationService.verifyKey(rawApiKey)).thenReturn(Mono.just(auth));

        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Phase 5: Should return 429 and Retry-After header when rate limit (RPS) is exceeded")
    void shouldReturn429WhenRateLimitExceeded() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/gateway/weather/v1/forecast")
                .header(HeaderConstants.X_API_KEY, rawApiKey)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ApiMetadata meta = ApiMetadata.builder()
                .apiId(apiId)
                .slug("weather")
                .targetBackendUrl("http://weather-service:8080")
                .status("PUBLISHED")
                .build();
        ApiKeyAuthData auth = ApiKeyAuthData.builder()
                .keyId(keyId)
                .apiId(apiId.toString())
                .rateLimitRps(5)
                .quotaMonthly(1000)
                .status("ACTIVE")
                .build();

        when(apiResolutionService.resolveApi("weather")).thenReturn(Mono.just(meta));
        when(apiKeyVerificationService.verifyKey(rawApiKey)).thenReturn(Mono.just(auth));
        when(rateLimitService.isAllowed(keyId, 5)).thenReturn(Mono.just(false)); // Limit exceeded

        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("1");
    }

    @Test
    @DisplayName("Phase 6: Should return 429 when monthly quota is exceeded")
    void shouldReturn429WhenMonthlyQuotaExceeded() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/gateway/weather/v1/forecast")
                .header(HeaderConstants.X_API_KEY, rawApiKey)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ApiMetadata meta = ApiMetadata.builder()
                .apiId(apiId)
                .slug("weather")
                .targetBackendUrl("http://weather-service:8080")
                .status("PUBLISHED")
                .build();
        ApiKeyAuthData auth = ApiKeyAuthData.builder()
                .keyId(keyId)
                .apiId(apiId.toString())
                .rateLimitRps(5)
                .quotaMonthly(100)
                .status("ACTIVE")
                .build();

        when(apiResolutionService.resolveApi("weather")).thenReturn(Mono.just(meta));
        when(apiKeyVerificationService.verifyKey(rawApiKey)).thenReturn(Mono.just(auth));
        when(rateLimitService.isAllowed(keyId, 5)).thenReturn(Mono.just(true));
        when(quotaService.checkAndIncrementQuota(keyId, 100)).thenReturn(Mono.just(false)); // Quota exceeded

        GatewayFilterChain chain = ex -> Mono.empty();

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("Phases 7, 8, 9: Full Pass-Through should mutate headers, rewrite destination URI, and record telemetry")
    void shouldMutateHeadersAndForwardToBackendOnSuccess() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/gateway/weather/v1/forecast?city=London")
                .header(HeaderConstants.X_API_KEY, rawApiKey)
                .header(HeaderConstants.X_CORRELATION_ID, "test-corr-abc")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ApiMetadata meta = ApiMetadata.builder()
                .apiId(apiId)
                .slug("weather")
                .targetBackendUrl("https://api.weather-provider.com")
                .status("PUBLISHED")
                .build();
        ApiKeyAuthData auth = ApiKeyAuthData.builder()
                .keyId(keyId)
                .developerId(developerId)
                .apiId(apiId.toString())
                .rateLimitRps(10)
                .quotaMonthly(1000)
                .status("ACTIVE")
                .build();

        when(apiResolutionService.resolveApi("weather")).thenReturn(Mono.just(meta));
        when(apiKeyVerificationService.verifyKey(rawApiKey)).thenReturn(Mono.just(auth));
        when(rateLimitService.isAllowed(keyId, 10)).thenReturn(Mono.just(true));
        when(quotaService.checkAndIncrementQuota(keyId, 1000)).thenReturn(Mono.just(true));

        AtomicBoolean downstreamCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = mutatedExchange -> {
            downstreamCalled.set(true);

            // Phase 7 checks:
            HttpHeaders headers = mutatedExchange.getRequest().getHeaders();
            assertThat(headers.getFirst(HeaderConstants.X_API_KEY)).isNull(); // Stripped!
            assertThat(headers.getFirst(HeaderConstants.X_CORRELATION_ID)).isEqualTo("test-corr-abc");
            assertThat(headers.getFirst(HeaderConstants.X_INTERNAL_TOKEN)).isNotBlank(); // Signed token!
            assertThat(headers.getFirst(HeaderConstants.X_USER_ID)).isEqualTo(developerId);

            // Phase 8 checks:
            URI targetUri = mutatedExchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
            assertThat(targetUri).isNotNull();
            assertThat(targetUri.toString()).isEqualTo("https://api.weather-provider.com/v1/forecast?city=London");

            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(downstreamCalled.get()).isTrue();
        // Phase 9 checks:
        verify(telemetryPublisher).publishTelemetry(any());
    }
}
