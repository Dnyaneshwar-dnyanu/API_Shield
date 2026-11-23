package com.apishield.gateway.filter;

import com.apishield.common.constant.HeaderConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdGlobalFilterTest {

    private CorrelationIdGlobalFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdGlobalFilter();
    }

    @Test
    @DisplayName("Should generate new correlation ID when request has none")
    void shouldGenerateCorrelationIdWhenMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<String> forwardedCorrelationId = new AtomicReference<>();
        GatewayFilterChain chain = mutatedExchange -> {
            forwardedCorrelationId.set(mutatedExchange.getRequest().getHeaders().getFirst(HeaderConstants.X_CORRELATION_ID));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(forwardedCorrelationId.get()).isNotNull().isNotBlank();
        assertThat(exchange.getResponse().getHeaders().getFirst(HeaderConstants.X_CORRELATION_ID))
                .isEqualTo(forwardedCorrelationId.get());
    }

    @Test
    @DisplayName("Should preserve existing correlation ID when present")
    void shouldPreserveExistingCorrelationId() {
        String existingId = "test-corr-12345";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/users")
                .header(HeaderConstants.X_CORRELATION_ID, existingId)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<String> forwardedCorrelationId = new AtomicReference<>();
        GatewayFilterChain chain = mutatedExchange -> {
            forwardedCorrelationId.set(mutatedExchange.getRequest().getHeaders().getFirst(HeaderConstants.X_CORRELATION_ID));
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(forwardedCorrelationId.get()).isEqualTo(existingId);
        assertThat(exchange.getResponse().getHeaders().getFirst(HeaderConstants.X_CORRELATION_ID))
                .isEqualTo(existingId);
    }
}
