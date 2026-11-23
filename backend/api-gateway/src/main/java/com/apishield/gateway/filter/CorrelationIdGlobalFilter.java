package com.apishield.gateway.filter;

import com.apishield.common.constant.HeaderConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(HeaderConstants.X_CORRELATION_ID);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            log.trace("Assigned new correlation ID: {}", correlationId);
        }

        final String finalCorrelationId = correlationId;

        // Mutate request with correlation ID header
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(HeaderConstants.X_CORRELATION_ID, finalCorrelationId)
                .build();

        // Mutate response to include correlation ID header
        exchange.getResponse().getHeaders().set(HeaderConstants.X_CORRELATION_ID, finalCorrelationId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
