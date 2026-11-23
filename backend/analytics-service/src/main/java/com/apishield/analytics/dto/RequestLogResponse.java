package com.apishield.analytics.dto;

import com.apishield.analytics.entity.RequestLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestLogResponse {
    private String requestId;
    private String apiId;
    private String apiSlug;
    private String endpointPath;
    private String httpMethod;
    private int statusCode;
    private int latencyMs;
    private String clientIp;
    private Instant timestamp;
    private String errorMessage;

    public static RequestLogResponse fromEntity(RequestLog entity) {
        return RequestLogResponse.builder()
                .requestId(entity.getRequestId())
                .apiId(entity.getApiId())
                .apiSlug(entity.getApiSlug())
                .endpointPath(entity.getEndpointPath())
                .httpMethod(entity.getHttpMethod())
                .statusCode(entity.getStatusCode())
                .latencyMs(entity.getLatencyMs())
                .clientIp(entity.getClientIp())
                .timestamp(entity.getTimestamp())
                .errorMessage(entity.getErrorMessage())
                .build();
    }
}
