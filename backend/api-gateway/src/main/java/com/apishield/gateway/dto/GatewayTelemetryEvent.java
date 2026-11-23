package com.apishield.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayTelemetryEvent {
    private String correlationId;
    private UUID apiId;
    private String apiSlug;
    private String developerId;
    private String subscriptionId;
    private UUID keyId;
    private String clientIp;
    private String httpMethod;
    private String endpointPath;
    private int statusCode;
    private long responseTimeMs;
    private Instant timestamp;
    private String errorMessage;
}
