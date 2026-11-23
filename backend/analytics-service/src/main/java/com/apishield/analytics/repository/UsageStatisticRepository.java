package com.apishield.analytics.repository;

import com.apishield.analytics.entity.UsageStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsageStatisticRepository extends JpaRepository<UsageStatistic, Long> {

    Optional<UsageStatistic> findByApiIdAndDeveloperIdAndPeriodDateAndPeriodHour(
            String apiId, String developerId, LocalDate periodDate, Integer periodHour);

    List<UsageStatistic> findByApiIdAndPeriodDateBetweenOrderByPeriodDateAscPeriodHourAsc(
            String apiId, LocalDate startDate, LocalDate endDate);

    List<UsageStatistic> findByDeveloperIdAndPeriodDateBetweenOrderByPeriodDateAscPeriodHourAsc(
            String developerId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COALESCE(SUM(u.totalRequests), 0) FROM UsageStatistic u WHERE u.developerId = :developerId AND u.periodDate >= :startDate")
    long sumTotalRequestsForDeveloperSince(@Param("developerId") String developerId, @Param("startDate") LocalDate startDate);

    @Query("SELECT COALESCE(SUM(u.totalRequests), 0) FROM UsageStatistic u WHERE u.apiId = :apiId AND u.periodDate >= :startDate")
    long sumTotalRequestsForApiSince(@Param("apiId") String apiId, @Param("startDate") LocalDate startDate);

    @Query("SELECT COALESCE(SUM(u.successfulRequests), 0) FROM UsageStatistic u WHERE u.apiId = :apiId AND u.periodDate >= :startDate")
    long sumSuccessfulRequestsForApiSince(@Param("apiId") String apiId, @Param("startDate") LocalDate startDate);

    @Query("SELECT COALESCE(SUM(u.clientErrors + u.serverErrors), 0) FROM UsageStatistic u WHERE u.apiId = :apiId AND u.periodDate >= :startDate")
    long sumErrorsForApiSince(@Param("apiId") String apiId, @Param("startDate") LocalDate startDate);

    @Query("SELECT COALESCE(AVG(u.avgLatencyMs), 0.0) FROM UsageStatistic u WHERE u.apiId = :apiId AND u.periodDate >= :startDate")
    double avgLatencyForApiSince(@Param("apiId") String apiId, @Param("startDate") LocalDate startDate);

    @Query("SELECT COALESCE(SUM(u.totalRequests), 0) FROM UsageStatistic u WHERE u.periodDate >= :startDate")
    long sumTotalRequestsPlatformSince(@Param("startDate") LocalDate startDate);

    @Query("SELECT COALESCE(SUM(u.successfulRequests), 0) FROM UsageStatistic u WHERE u.periodDate >= :startDate")
    long sumSuccessfulRequestsPlatformSince(@Param("startDate") LocalDate startDate);

    @Query("SELECT COALESCE(SUM(u.clientErrors + u.serverErrors), 0) FROM UsageStatistic u WHERE u.periodDate >= :startDate")
    long sumErrorsPlatformSince(@Param("startDate") LocalDate startDate);

    @Query("SELECT COALESCE(AVG(u.avgLatencyMs), 0.0) FROM UsageStatistic u WHERE u.periodDate >= :startDate")
    double avgLatencyPlatformSince(@Param("startDate") LocalDate startDate);
}
