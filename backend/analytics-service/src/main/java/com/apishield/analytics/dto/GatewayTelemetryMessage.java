package com.apishield.analytics.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GatewayTelemetryMessage {
    private String correlationId;
    private String apiId;
    private String apiSlug;
    private String developerId;
    private String subscriptionId;
    private String keyId;
    private String clientIp;
    private String httpMethod;
    private String endpointPath;
    private int statusCode;
    private long responseTimeMs;
    private Instant timestamp;
    private String errorMessage;
}
