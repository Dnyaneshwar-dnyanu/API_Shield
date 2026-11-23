package com.apishield.analytics.service;

import com.apishield.analytics.dto.DeveloperQuotaResponse;
import com.apishield.analytics.dto.OwnerOverviewResponse;
import com.apishield.analytics.dto.RequestLogResponse;
import com.apishield.analytics.dto.TimeseriesPointResponse;
import com.apishield.analytics.entity.UsageStatistic;
import com.apishield.analytics.repository.RequestLogRepository;
import com.apishield.analytics.repository.UsageStatisticRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsQueryService {

    private final UsageStatisticRepository usageStatisticRepository;
    private final RequestLogRepository requestLogRepository;

    public OwnerOverviewResponse getOwnerOverview(String ownerId, String apiId, Integer days) {
        int windowDays = (days != null && days > 0) ? days : 30;
        LocalDate startDate = LocalDate.now(ZoneOffset.UTC).minusDays(windowDays);
        Instant sinceInstant = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();

        long totalRequests;
        long successfulRequests;
        long errorCount;
        double avgLatency;
        List<Integer> latencies;

        if (apiId != null && !apiId.isBlank()) {
            totalRequests = usageStatisticRepository.sumTotalRequestsForApiSince(apiId, startDate);
            successfulRequests = usageStatisticRepository.sumSuccessfulRequestsForApiSince(apiId, startDate);
            errorCount = usageStatisticRepository.sumErrorsForApiSince(apiId, startDate);
            avgLatency = usageStatisticRepository.avgLatencyForApiSince(apiId, startDate);
            latencies = requestLogRepository.findLatenciesForApiSince(apiId, sinceInstant);
        } else {
            totalRequests = usageStatisticRepository.sumTotalRequestsPlatformSince(startDate);
            successfulRequests = usageStatisticRepository.sumSuccessfulRequestsPlatformSince(startDate);
            errorCount = usageStatisticRepository.sumErrorsPlatformSince(startDate);
            avgLatency = usageStatisticRepository.avgLatencyPlatformSince(startDate);
            latencies = requestLogRepository.findAllLatenciesSince(sinceInstant);
        }

        double errorRate = totalRequests > 0 ? ((double) errorCount / totalRequests) * 100.0 : 0.0;

        int p95Latency = 0;
        if (!latencies.isEmpty()) {
            int p95Index = (int) Math.ceil(0.95 * latencies.size()) - 1;
            int safeIndex = Math.max(0, Math.min(p95Index, latencies.size() - 1));
            p95Latency = latencies.get(safeIndex);
        } else if (avgLatency > 0) {
            p95Latency = (int) Math.round(avgLatency * 1.5);
        }

        return OwnerOverviewResponse.builder()
                .totalRequests(totalRequests)
                .successfulRequests(successfulRequests)
                .errorCount(errorCount)
                .errorRate(Math.round(errorRate * 100.0) / 100.0)
                .averageLatencyMs(Math.round(avgLatency * 10.0) / 10.0)
                .p95LatencyMs(p95Latency)
                .build();
    }

    public List<TimeseriesPointResponse> getTimeseries(String apiId, Integer days) {
        if (apiId == null || apiId.isBlank()) {
            return Collections.emptyList();
        }

        int windowDays = (days != null && days > 0) ? days : 7;
        LocalDate startDate = LocalDate.now(ZoneOffset.UTC).minusDays(windowDays);
        LocalDate endDate = LocalDate.now(ZoneOffset.UTC);

        List<UsageStatistic> stats = usageStatisticRepository
                .findByApiIdAndPeriodDateBetweenOrderByPeriodDateAscPeriodHourAsc(apiId, startDate, endDate);

        return stats.stream()
                .map(stat -> {
                    String ts = stat.getPeriodHour() != null
                            ? String.format("%sT%02d:00:00Z", stat.getPeriodDate(), stat.getPeriodHour())
                            : stat.getPeriodDate().toString();
                    return TimeseriesPointResponse.builder()
                            .timestamp(ts)
                            .totalRequests(stat.getTotalRequests())
                            .successfulRequests(stat.getSuccessfulRequests())
                            .clientErrors(stat.getClientErrors())
                            .serverErrors(stat.getServerErrors())
                            .avgLatencyMs(stat.getAvgLatencyMs())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public DeveloperQuotaResponse getDeveloperQuota(String developerId) {
        String devId = (developerId != null && !developerId.isBlank()) ? developerId : "anonymous";
        LocalDate startOfMonth = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        long currentMonthUsage = usageStatisticRepository.sumTotalRequestsForDeveloperSince(devId, startOfMonth);

        return DeveloperQuotaResponse.builder()
                .developerId(devId)
                .currentMonthUsage(currentMonthUsage)
                .period(startOfMonth + " to " + LocalDate.now(ZoneOffset.UTC))
                .build();
    }

    public Page<RequestLogResponse> getDeveloperLogs(String developerId, String apiId, Pageable pageable) {
        String devId = (developerId != null && !developerId.isBlank()) ? developerId : "anonymous";
        if (apiId != null && !apiId.isBlank()) {
            return requestLogRepository.findByDeveloperIdAndApiIdOrderByTimestampDesc(devId, apiId, pageable)
                    .map(RequestLogResponse::fromEntity);
        }
        return requestLogRepository.findByDeveloperIdOrderByTimestampDesc(devId, pageable)
                .map(RequestLogResponse::fromEntity);
    }
}
