package com.apishield.analytics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "usage_statistics", indexes = {
        @Index(name = "idx_usage_api_date", columnList = "api_id, period_date"),
        @Index(name = "idx_usage_dev_date", columnList = "developer_id, period_date")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_api_dev_period", columnNames = {"api_id", "developer_id", "period_date", "period_hour"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsageStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "api_id", nullable = false, length = 36)
    private String apiId;

    @Column(name = "developer_id", nullable = false, length = 36)
    private String developerId;

    @Column(name = "plan_id", length = 36)
    private String planId;

    @Column(name = "period_date", nullable = false)
    private LocalDate periodDate;

    @Column(name = "period_hour")
    private Integer periodHour;

    @Column(name = "total_requests", nullable = false)
    @Builder.Default
    private long totalRequests = 0;

    @Column(name = "successful_requests", nullable = false)
    @Builder.Default
    private long successfulRequests = 0;

    @Column(name = "client_errors", nullable = false)
    @Builder.Default
    private long clientErrors = 0;

    @Column(name = "server_errors", nullable = false)
    @Builder.Default
    private long serverErrors = 0;

    @Column(name = "blocked_rate_limit", nullable = false)
    @Builder.Default
    private long blockedRateLimit = 0;

    @Column(name = "blocked_auth", nullable = false)
    @Builder.Default
    private long blockedAuth = 0;

    @Column(name = "avg_latency_ms", nullable = false)
    @Builder.Default
    private int avgLatencyMs = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
