package com.apishield.analytics.service;

import com.apishield.analytics.dto.DeveloperQuotaResponse;
import com.apishield.analytics.dto.OwnerOverviewResponse;
import com.apishield.analytics.dto.RequestLogResponse;
import com.apishield.analytics.dto.TimeseriesPointResponse;
import com.apishield.analytics.entity.RequestLog;
import com.apishield.analytics.entity.UsageStatistic;
import com.apishield.analytics.repository.RequestLogRepository;
import com.apishield.analytics.repository.UsageStatisticRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsQueryServiceTest {

    @Mock
    private UsageStatisticRepository usageStatisticRepository;

    @Mock
    private RequestLogRepository requestLogRepository;

    @InjectMocks
    private AnalyticsQueryService queryService;

    @Test
    @DisplayName("Should calculate owner overview statistics with p95 latency")
    void shouldCalculateOwnerOverview() {
        when(usageStatisticRepository.sumTotalRequestsForApiSince(eq("api-1"), any(LocalDate.class))).thenReturn(100L);
        when(usageStatisticRepository.sumSuccessfulRequestsForApiSince(eq("api-1"), any(LocalDate.class))).thenReturn(95L);
        when(usageStatisticRepository.sumErrorsForApiSince(eq("api-1"), any(LocalDate.class))).thenReturn(5L);
        when(usageStatisticRepository.avgLatencyForApiSince(eq("api-1"), any(LocalDate.class))).thenReturn(42.5);
        when(requestLogRepository.findLatenciesForApiSince(eq("api-1"), any(Instant.class)))
                .thenReturn(List.of(10, 20, 30, 40, 50, 60, 70, 80, 90, 150));

        OwnerOverviewResponse overview = queryService.getOwnerOverview("owner-1", "api-1", 30);

        assertThat(overview.getTotalRequests()).isEqualTo(100L);
        assertThat(overview.getSuccessfulRequests()).isEqualTo(95L);
        assertThat(overview.getErrorCount()).isEqualTo(5L);
        assertThat(overview.getErrorRate()).isEqualTo(5.0);
        assertThat(overview.getAverageLatencyMs()).isEqualTo(42.5);
        assertThat(overview.getP95LatencyMs()).isEqualTo(150);
    }

    @Test
    @DisplayName("Should return timeseries data points for API")
    void shouldReturnTimeseries() {
        UsageStatistic stat1 = UsageStatistic.builder()
                .apiId("api-1")
                .developerId("dev-1")
                .periodDate(LocalDate.of(2026, 11, 23))
                .periodHour(10)
                .totalRequests(50)
                .successfulRequests(48)
                .clientErrors(2)
                .serverErrors(0)
                .avgLatencyMs(35)
                .build();

        when(usageStatisticRepository.findByApiIdAndPeriodDateBetweenOrderByPeriodDateAscPeriodHourAsc(
                eq("api-1"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(stat1));

        List<TimeseriesPointResponse> result = queryService.getTimeseries("api-1", 7);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTimestamp()).isEqualTo("2026-11-23T10:00:00Z");
        assertThat(result.get(0).getTotalRequests()).isEqualTo(50L);
        assertThat(result.get(0).getSuccessfulRequests()).isEqualTo(48L);
    }

    @Test
    @DisplayName("Should return developer quota usage for current month")
    void shouldReturnDeveloperQuota() {
        when(usageStatisticRepository.sumTotalRequestsForDeveloperSince(eq("dev-1"), any(LocalDate.class)))
                .thenReturn(12500L);

        DeveloperQuotaResponse quota = queryService.getDeveloperQuota("dev-1");

        assertThat(quota.getDeveloperId()).isEqualTo("dev-1");
        assertThat(quota.getCurrentMonthUsage()).isEqualTo(12500L);
        assertThat(quota.getPeriod()).contains("2026-11-01");
    }

    @Test
    @DisplayName("Should return paginated developer request logs")
    void shouldReturnDeveloperLogs() {
        RequestLog log1 = RequestLog.builder()
                .id(1L)
                .requestId("req-1")
                .apiId("api-1")
                .apiSlug("test-api")
                .developerId("dev-1")
                .endpointPath("/test")
                .httpMethod("GET")
                .statusCode(200)
                .latencyMs(25)
                .clientIp("127.0.0.1")
                .timestamp(Instant.now())
                .build();

        Page<RequestLog> page = new PageImpl<>(List.of(log1));
        when(requestLogRepository.findByDeveloperIdOrderByTimestampDesc(eq("dev-1"), any(PageRequest.class)))
                .thenReturn(page);

        Page<RequestLogResponse> response = queryService.getDeveloperLogs("dev-1", null, PageRequest.of(0, 10));

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getRequestId()).isEqualTo("req-1");
        assertThat(response.getContent().get(0).getStatusCode()).isEqualTo(200);
    }
}
