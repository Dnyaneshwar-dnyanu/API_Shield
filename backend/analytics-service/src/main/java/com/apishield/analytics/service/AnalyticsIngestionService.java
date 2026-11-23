package com.apishield.analytics.service;

import com.apishield.analytics.dto.GatewayTelemetryMessage;
import com.apishield.analytics.entity.RequestLog;
import com.apishield.analytics.entity.UsageStatistic;
import com.apishield.analytics.repository.RequestLogRepository;
import com.apishield.analytics.repository.UsageStatisticRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsIngestionService {

    private final RequestLogRepository requestLogRepository;
    private final UsageStatisticRepository usageStatisticRepository;

    @Transactional
    public void ingest(GatewayTelemetryMessage message) {
        if (message == null) {
            return;
        }

        String requestId = message.getCorrelationId();
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        Instant timestamp = message.getTimestamp() != null ? message.getTimestamp() : Instant.now();
        int latencyMs = (int) Math.min(message.getResponseTimeMs(), Integer.MAX_VALUE);
        String apiId = message.getApiId() != null ? message.getApiId() : "unknown-api";
        String developerId = message.getDeveloperId() != null ? message.getDeveloperId() : "anonymous";

        // 1. Persist Raw Request Log
        RequestLog requestLog = RequestLog.builder()
                .requestId(requestId)
                .apiId(apiId)
                .apiSlug(message.getApiSlug())
                .developerId(message.getDeveloperId())
                .subscriptionId(message.getSubscriptionId())
                .apiKeyId(message.getKeyId())
                .endpointPath(message.getEndpointPath() != null ? message.getEndpointPath() : "/")
                .httpMethod(message.getHttpMethod() != null ? message.getHttpMethod() : "GET")
                .statusCode(message.getStatusCode())
                .latencyMs(latencyMs)
                .clientIp(message.getClientIp())
                .errorMessage(message.getErrorMessage())
                .timestamp(timestamp)
                .build();

        try {
            requestLogRepository.save(requestLog);
        } catch (Exception e) {
            log.warn("Failed to persist request log for correlationId {}: {}", requestId, e.getMessage());
        }

        // 2. Update Atomic Usage Statistics Rollup
        ZonedDateTime zdt = timestamp.atZone(ZoneOffset.UTC);
        LocalDate periodDate = zdt.toLocalDate();
        int periodHour = zdt.getHour();

        UsageStatistic stat = usageStatisticRepository
                .findByApiIdAndDeveloperIdAndPeriodDateAndPeriodHour(apiId, developerId, periodDate, periodHour)
                .orElseGet(() -> UsageStatistic.builder()
                        .apiId(apiId)
                        .developerId(developerId)
                        .planId(message.getSubscriptionId())
                        .periodDate(periodDate)
                        .periodHour(periodHour)
                        .totalRequests(0)
                        .successfulRequests(0)
                        .clientErrors(0)
                        .serverErrors(0)
                        .blockedRateLimit(0)
                        .blockedAuth(0)
                        .avgLatencyMs(0)
                        .build());

        long prevTotal = stat.getTotalRequests();
        long newTotal = prevTotal + 1;
        stat.setTotalRequests(newTotal);

        int statusCode = message.getStatusCode();
        if (statusCode >= 200 && statusCode < 300) {
            stat.setSuccessfulRequests(stat.getSuccessfulRequests() + 1);
        } else if (statusCode == 429) {
            stat.setBlockedRateLimit(stat.getBlockedRateLimit() + 1);
            stat.setClientErrors(stat.getClientErrors() + 1);
        } else if (statusCode == 401 || statusCode == 403) {
            stat.setBlockedAuth(stat.getBlockedAuth() + 1);
            stat.setClientErrors(stat.getClientErrors() + 1);
        } else if (statusCode >= 400 && statusCode < 500) {
            stat.setClientErrors(stat.getClientErrors() + 1);
        } else if (statusCode >= 500) {
            stat.setServerErrors(stat.getServerErrors() + 1);
        }

        // Rolling average latency calculation
        int updatedAvgLatency = (int) (((stat.getAvgLatencyMs() * prevTotal) + latencyMs) / newTotal);
        stat.setAvgLatencyMs(updatedAvgLatency);

        usageStatisticRepository.save(stat);
        log.debug("Ingested telemetry: apiId={}, status={}, latency={}ms", apiId, statusCode, latencyMs);
    }
}
