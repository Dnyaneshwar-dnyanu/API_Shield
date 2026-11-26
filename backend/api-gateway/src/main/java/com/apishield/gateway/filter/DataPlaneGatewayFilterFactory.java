package com.apishield.gateway.filter;

import com.apishield.common.constant.HeaderConstants;
import com.apishield.common.dto.ProblemDetailResponse;
import com.apishield.common.util.CryptoUtils;
import com.apishield.gateway.config.ApiShieldGatewayProperties;
import com.apishield.gateway.dto.ApiKeyAuthData;
import com.apishield.gateway.dto.ApiMetadata;
import com.apishield.gateway.dto.GatewayTelemetryEvent;
import com.apishield.gateway.service.ApiKeyVerificationService;
import com.apishield.gateway.service.ApiResolutionService;
import com.apishield.gateway.service.GatewayTelemetryPublisher;
import com.apishield.gateway.service.QuotaService;
import com.apishield.gateway.service.RateLimitService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executes the complete 9-phase Data Plane Proxy Lifecycle:
 * Phase 1: Request Identification & Correlation ID Injection
 * Phase 2: Route Resolution & Active Verification
 * Phase 3: Client Authentication & Cryptographic Key Lookup
 * Phase 4: API Scope & Key Status Verification
 * Phase 5: Token Bucket Rate Limiting (RPS)
 * Phase 6: Monthly Quota Enforcement
 * Phase 7: Header Mutation & Gateway Origin Signing
 * Phase 8: Non-blocking Downstream Dispatch
 * Phase 9: Telemetry & Access Log Asynchronous Publishing
 */
@Slf4j
@Component
public class DataPlaneGatewayFilterFactory extends AbstractGatewayFilterFactory<DataPlaneGatewayFilterFactory.Config> {

    private static final Pattern GATEWAY_PATH_PATTERN = Pattern.compile("^/gateway/([^/?#]+)(.*)$");
    private static final int FILTER_ORDER = 10001; // Runs after RouteToRequestUrlFilter (10000)

    private final ApiResolutionService apiResolutionService;
    private final ApiKeyVerificationService apiKeyVerificationService;
    private final RateLimitService rateLimitService;
    private final QuotaService quotaService;
    private final GatewayTelemetryPublisher telemetryPublisher;
    private final ApiShieldGatewayProperties gatewayProperties;
    private final ObjectMapper objectMapper;

    public DataPlaneGatewayFilterFactory(
            ApiResolutionService apiResolutionService,
            ApiKeyVerificationService apiKeyVerificationService,
            RateLimitService rateLimitService,
            QuotaService quotaService,
            GatewayTelemetryPublisher telemetryPublisher,
            ApiShieldGatewayProperties gatewayProperties,
            ObjectMapper objectMapper) {
        super(Config.class);
        this.apiResolutionService = apiResolutionService;
        this.apiKeyVerificationService = apiKeyVerificationService;
        this.rateLimitService = rateLimitService;
        this.quotaService = quotaService;
        this.telemetryPublisher = telemetryPublisher;
        this.gatewayProperties = gatewayProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter(this::processRequest, FILTER_ORDER);
    }

    private Mono<Void> processRequest(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();
        String fullPath = request.getPath().value();

        // Phase 1: Extract Correlation ID
        String correlationId = request.getHeaders().getFirst(HeaderConstants.X_CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        final String finalCorrelationId = correlationId;

        // Extract {apiSlug} and subPath
        Matcher matcher = GATEWAY_PATH_PATTERN.matcher(fullPath);
        if (!matcher.matches()) {
            return writeErrorResponse(exchange, HttpStatus.BAD_REQUEST, "Invalid Gateway Path",
                    "Path must follow pattern /gateway/{apiSlug}/**", finalCorrelationId, null, null, null, null, startTime);
        }

        String apiSlug = matcher.group(1);
        String subPath = matcher.group(2);
        if (subPath == null || subPath.isBlank()) {
            subPath = "/";
        }

        final String finalSubPath = subPath;

        // Phase 2: Route Resolution & Status Check
        return apiResolutionService.resolveApi(apiSlug)
                .switchIfEmpty(Mono.error(new GatewayException(HttpStatus.NOT_FOUND, "API Not Found",
                        "No published API found matching slug: " + apiSlug)))
                .flatMap(apiMetadata -> {
                    if (!"PUBLISHED".equalsIgnoreCase(apiMetadata.getStatus())) {
                        return Mono.error(new GatewayException(HttpStatus.NOT_FOUND, "API Not Available",
                                "API is not currently published (status: " + apiMetadata.getStatus() + ")"));
                    }

                    // Phase 3: Client Authentication & Key Lookup
                    String rawApiKey = request.getHeaders().getFirst(HeaderConstants.X_API_KEY);
                    if (rawApiKey == null || rawApiKey.isBlank()) {
                        return Mono.error(new GatewayException(HttpStatus.UNAUTHORIZED, "Missing API Key",
                                "A valid 'X-API-Key' header is required to access this endpoint."));
                    }

                    return apiKeyVerificationService.verifyKey(rawApiKey)
                            .switchIfEmpty(Mono.error(new GatewayException(HttpStatus.UNAUTHORIZED, "Invalid API Key",
                                    "The provided API key is invalid, revoked, or unrecognized.")))
                            .flatMap(authData -> {
                                // Phase 4: API Scope & Status Check
                                if (!"ACTIVE".equalsIgnoreCase(authData.getStatus())) {
                                    return Mono.error(new GatewayException(HttpStatus.FORBIDDEN, "API Key Inactive",
                                            "API key is inactive (current status: " + authData.getStatus() + ")."));
                                }

                                if (authData.getApiId() != null && apiMetadata.getApiId() != null
                                        && !authData.getApiId().equalsIgnoreCase(apiMetadata.getApiId().toString())) {
                                    return Mono.error(new GatewayException(HttpStatus.FORBIDDEN, "Unauthorized API Access",
                                            "API key does not grant authorization for API: " + apiSlug));
                                }

                                // Phase 5: Token Bucket Rate Limiting (RPS)
                                return rateLimitService.isAllowed(authData.getKeyId(), authData.getRateLimitRps())
                                        .flatMap(allowedRps -> {
                                            if (!allowedRps) {
                                                exchange.getResponse().getHeaders().set(HttpHeaders.RETRY_AFTER, "1");
                                                return Mono.error(new GatewayException(HttpStatus.TOO_MANY_REQUESTS, "Rate Limit Exceeded",
                                                        "Rate limit of " + authData.getRateLimitRps() + " RPS exceeded. Please retry after delay."));
                                            }

                                            // Phase 6: Monthly Quota Enforcement
                                            return quotaService.checkAndIncrementQuota(authData.getKeyId(), authData.getQuotaMonthly())
                                                    .flatMap(allowedQuota -> {
                                                        if (!allowedQuota) {
                                                            return Mono.error(new GatewayException(HttpStatus.TOO_MANY_REQUESTS, "Monthly Quota Exceeded",
                                                                    "Monthly quota of " + authData.getQuotaMonthly() + " requests exceeded."));
                                                        }

                                                        // Phase 7: Header Mutation & Origin Signing
                                                        String internalSecret = gatewayProperties.getInternalSecret();
                                                        String signPayload = authData.getDeveloperId() + ":" + apiMetadata.getApiId() + ":" + finalCorrelationId;
                                                        String internalToken = CryptoUtils.hmacSha256Hex(signPayload, internalSecret);

                                                        ServerHttpRequest.Builder reqBuilder = exchange.getRequest().mutate()
                                                                .headers(httpHeaders -> {
                                                                    httpHeaders.remove(HeaderConstants.X_API_KEY);
                                                                    httpHeaders.set(HeaderConstants.X_CORRELATION_ID, finalCorrelationId);
                                                                    httpHeaders.set(HeaderConstants.X_INTERNAL_TOKEN, internalToken);
                                                                    if (authData.getDeveloperId() != null) {
                                                                        httpHeaders.set(HeaderConstants.X_USER_ID, authData.getDeveloperId());
                                                                    }
                                                                });

                                                        // Phase 8: Rewrite Destination URI
                                                        String targetBase = apiMetadata.getTargetBackendUrl().replaceAll("/+$", "");
                                                        String rawQuery = exchange.getRequest().getURI().getRawQuery();
                                                        String targetUrlStr = targetBase + (finalSubPath.startsWith("/") ? finalSubPath : "/" + finalSubPath)
                                                                + (rawQuery != null && !rawQuery.isBlank() ? "?" + rawQuery : "");

                                                        URI targetUri = URI.create(targetUrlStr);
                                                        reqBuilder.uri(targetUri);
                                                        ServerHttpRequest mutatedRequest = reqBuilder.build();

                                                        ServerWebExchange mutatedExchange = exchange.mutate()
                                                                .request(mutatedRequest)
                                                                .build();

                                                        // Set gateway routing attributes so Netty sends directly to targetUri
                                                        mutatedExchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, targetUri);

                                                        // Phase 9: Telemetry & Access Log publishing on completion
                                                        return chain.filter(mutatedExchange)
                                                                .doFinally(signalType -> {
                                                                    long duration = System.currentTimeMillis() - startTime;
                                                                    int statusCode = mutatedExchange.getResponse().getStatusCode() != null
                                                                            ? mutatedExchange.getResponse().getStatusCode().value()
                                                                            : 200;
                                                                    publishTelemetry(finalCorrelationId, apiMetadata, authData,
                                                                            request, statusCode, duration, null);
                                                                });
                                                    });
                                        });
                            });
                })
                .onErrorResume(GatewayException.class, ex -> {
                    return writeErrorResponse(exchange, ex.getStatus(), ex.getTitle(), ex.getMessage(),
                            finalCorrelationId, apiSlug, null, null, null, startTime);
                })
                .onErrorResume(e -> {
                    log.error("Unhandled error in Data Plane proxy: {}", e.getMessage(), e);
                    return writeErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Gateway Error",
                            "An unexpected error occurred while processing the request.",
                            finalCorrelationId, apiSlug, null, null, e.getMessage(), startTime);
                });
    }

    private Mono<Void> writeErrorResponse(
            ServerWebExchange exchange,
            HttpStatus status,
            String title,
            String detail,
            String correlationId,
            String apiSlug,
            ApiMetadata metadata,
            ApiKeyAuthData authData,
            String errorMessage,
            long startTime) {

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        response.getHeaders().set(HeaderConstants.X_CORRELATION_ID, correlationId);

        ProblemDetailResponse problem = ProblemDetailResponse.builder()
                .type("urn:apishield:error:" + status.value())
                .status(status.value())
                .title(title)
                .detail(detail)
                .instance(exchange.getRequest().getPath().value())
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(problem);
        } catch (JsonProcessingException e) {
            bytes = ("{\"status\":" + status.value() + ",\"title\":\"" + title + "\",\"detail\":\"" + detail + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        long duration = System.currentTimeMillis() - startTime;
        publishTelemetry(correlationId, metadata, authData, exchange.getRequest(), status.value(), duration, title + ": " + detail);

        return response.writeWith(Mono.just(buffer));
    }

    private void publishTelemetry(
            String correlationId,
            ApiMetadata apiMetadata,
            ApiKeyAuthData authData,
            ServerHttpRequest request,
            int statusCode,
            long responseTimeMs,
            String errorMessage) {

        String clientIp = "unknown";
        if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            clientIp = request.getRemoteAddress().getAddress().getHostAddress();
        }

        GatewayTelemetryEvent event = GatewayTelemetryEvent.builder()
                .correlationId(correlationId)
                .apiId(apiMetadata != null ? apiMetadata.getApiId() : (authData != null && authData.getApiId() != null ? UUID.fromString(authData.getApiId()) : null))
                .apiSlug(apiMetadata != null ? apiMetadata.getSlug() : null)
                .developerId(authData != null ? authData.getDeveloperId() : null)
                .subscriptionId(authData != null ? authData.getSubscriptionId() : null)
                .keyId(authData != null ? authData.getKeyId() : null)
                .clientIp(clientIp)
                .httpMethod(request.getMethod().name())
                .endpointPath(request.getPath().value())
                .statusCode(statusCode)
                .responseTimeMs(responseTimeMs)
                .timestamp(Instant.now())
                .errorMessage(errorMessage)
                .build();

        telemetryPublisher.publishTelemetry(event);
    }

    public static class Config {
        // Configuration properties if needed in future
    }

    private static class GatewayException extends RuntimeException {
        private final HttpStatus status;
        private final String title;

        public GatewayException(HttpStatus status, String title, String detail) {
            super(detail);
            this.status = status;
            this.title = title;
        }

        public HttpStatus getStatus() {
            return status;
        }

        public String getTitle() {
            return title;
        }
    }
}
