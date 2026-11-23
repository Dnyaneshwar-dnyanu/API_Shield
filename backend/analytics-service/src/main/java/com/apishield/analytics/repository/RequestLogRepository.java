package com.apishield.analytics.repository;

import com.apishield.analytics.entity.RequestLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {

    Optional<RequestLog> findByRequestId(String requestId);

    Page<RequestLog> findByDeveloperIdOrderByTimestampDesc(String developerId, Pageable pageable);

    Page<RequestLog> findByDeveloperIdAndApiIdOrderByTimestampDesc(String developerId, String apiId, Pageable pageable);

    Page<RequestLog> findByApiIdOrderByTimestampDesc(String apiId, Pageable pageable);

    List<RequestLog> findByApiIdAndTimestampBetweenOrderByTimestampAsc(String apiId, Instant from, Instant to);

    @Query("SELECT r.latencyMs FROM RequestLog r WHERE r.apiId = :apiId AND r.timestamp >= :since ORDER BY r.latencyMs ASC")
    List<Integer> findLatenciesForApiSince(@Param("apiId") String apiId, @Param("since") Instant since);

    @Query("SELECT r.latencyMs FROM RequestLog r WHERE r.timestamp >= :since ORDER BY r.latencyMs ASC")
    List<Integer> findAllLatenciesSince(@Param("since") Instant since);
}
