package com.apishield.analytics.service;

import com.apishield.analytics.dto.GatewayTelemetryMessage;
import com.apishield.analytics.entity.RequestLog;
import com.apishield.analytics.entity.UsageStatistic;
import com.apishield.analytics.repository.RequestLogRepository;
import com.apishield.analytics.repository.UsageStatisticRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsIngestionServiceTest {

    @Mock
    private RequestLogRepository requestLogRepository;

    @Mock
    private UsageStatisticRepository usageStatisticRepository;

    @InjectMocks
    private AnalyticsIngestionService ingestionService;

    @Test
    @DisplayName("Should ingest telemetry and create new usage statistics rollup")
    void shouldIngestTelemetryAndCreateNewStatistic() {
        GatewayTelemetryMessage message = GatewayTelemetryMessage.builder()
                .correlationId("corr-123")
                .apiId("api-001")
                .apiSlug("orders-api")
                .developerId("dev-456")
                .subscriptionId("sub-789")
                .endpointPath("/v1/orders")
                .httpMethod("GET")
                .statusCode(200)
                .responseTimeMs(50)
                .timestamp(Instant.now())
                .build();

        when(usageStatisticRepository.findByApiIdAndDeveloperIdAndPeriodDateAndPeriodHour(
                eq("api-001"), eq("dev-456"), any(LocalDate.class), anyInt()))
                .thenReturn(Optional.empty());

        ingestionService.ingest(message);

        verify(requestLogRepository, times(1)).save(any(RequestLog.class));

        ArgumentCaptor<UsageStatistic> statCaptor = ArgumentCaptor.forClass(UsageStatistic.class);
        verify(usageStatisticRepository, times(1)).save(statCaptor.capture());

        UsageStatistic savedStat = statCaptor.getValue();
        assertThat(savedStat.getApiId()).isEqualTo("api-001");
        assertThat(savedStat.getDeveloperId()).isEqualTo("dev-456");
        assertThat(savedStat.getTotalRequests()).isEqualTo(1);
        assertThat(savedStat.getSuccessfulRequests()).isEqualTo(1);
        assertThat(savedStat.getClientErrors()).isEqualTo(0);
        assertThat(savedStat.getServerErrors()).isEqualTo(0);
        assertThat(savedStat.getAvgLatencyMs()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should ingest telemetry and update existing usage statistics with rate limit error")
    void shouldIngestTelemetryAndUpdateExistingStatisticWith429() {
        GatewayTelemetryMessage message = GatewayTelemetryMessage.builder()
                .correlationId("corr-429")
                .apiId("api-001")
                .developerId("dev-456")
                .endpointPath("/v1/orders")
                .httpMethod("POST")
                .statusCode(429)
                .responseTimeMs(10)
                .timestamp(Instant.now())
                .build();

        UsageStatistic existing = UsageStatistic.builder()
                .apiId("api-001")
                .developerId("dev-456")
                .periodDate(LocalDate.now())
                .periodHour(12)
                .totalRequests(9)
                .successfulRequests(9)
                .clientErrors(0)
                .serverErrors(0)
                .blockedRateLimit(0)
                .avgLatencyMs(50)
                .build();

        when(usageStatisticRepository.findByApiIdAndDeveloperIdAndPeriodDateAndPeriodHour(
                eq("api-001"), eq("dev-456"), any(LocalDate.class), anyInt()))
                .thenReturn(Optional.of(existing));

        ingestionService.ingest(message);

        ArgumentCaptor<UsageStatistic> statCaptor = ArgumentCaptor.forClass(UsageStatistic.class);
        verify(usageStatisticRepository, times(1)).save(statCaptor.capture());

        UsageStatistic updated = statCaptor.getValue();
        assertThat(updated.getTotalRequests()).isEqualTo(10);
        assertThat(updated.getSuccessfulRequests()).isEqualTo(9);
        assertThat(updated.getClientErrors()).isEqualTo(1);
        assertThat(updated.getBlockedRateLimit()).isEqualTo(1);
        // ((50 * 9) + 10) / 10 = 460 / 10 = 46
        assertThat(updated.getAvgLatencyMs()).isEqualTo(46);
    }

    @Test
    @DisplayName("Should handle 500 server error and record serverErrors")
    void shouldRecordServerError() {
        GatewayTelemetryMessage message = GatewayTelemetryMessage.builder()
                .correlationId("corr-500")
                .apiId("api-002")
                .developerId("dev-111")
                .statusCode(500)
                .responseTimeMs(200)
                .timestamp(Instant.now())
                .build();

        when(usageStatisticRepository.findByApiIdAndDeveloperIdAndPeriodDateAndPeriodHour(any(), any(), any(), anyInt()))
                .thenReturn(Optional.empty());

        ingestionService.ingest(message);

        ArgumentCaptor<UsageStatistic> statCaptor = ArgumentCaptor.forClass(UsageStatistic.class);
        verify(usageStatisticRepository).save(statCaptor.capture());

        UsageStatistic saved = statCaptor.getValue();
        assertThat(saved.getServerErrors()).isEqualTo(1);
        assertThat(saved.getSuccessfulRequests()).isEqualTo(0);
        assertThat(saved.getClientErrors()).isEqualTo(0);
    }
}
