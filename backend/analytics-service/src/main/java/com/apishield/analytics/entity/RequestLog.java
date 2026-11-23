package com.apishield.analytics.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "request_logs", indexes = {
        @Index(name = "idx_log_api_id", columnList = "api_id"),
        @Index(name = "idx_log_developer_id", columnList = "developer_id"),
        @Index(name = "idx_log_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true, length = 36)
    private String requestId;

    @Column(name = "api_id", nullable = false, length = 36)
    private String apiId;

    @Column(name = "api_slug", length = 100)
    private String apiSlug;

    @Column(name = "developer_id", length = 36)
    private String developerId;

    @Column(name = "subscription_id", length = 36)
    private String subscriptionId;

    @Column(name = "api_key_id", length = 36)
    private String apiKeyId;

    @Column(name = "endpoint_path", nullable = false, length = 500)
    private String endpointPath;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @Column(name = "latency_ms", nullable = false)
    private int latencyMs;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "timestamp", nullable = false)
    @Builder.Default
    private Instant timestamp = Instant.now();
}
