package com.apishield.apikey.entity;

import com.apishield.apikey.model.KeyStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_keys", indexes = {
        @Index(name = "idx_api_keys_developer_id", columnList = "developer_id"),
        @Index(name = "idx_api_keys_subscription_id", columnList = "subscription_id"),
        @Index(name = "idx_api_keys_api_id", columnList = "api_id"),
        @Index(name = "idx_api_keys_key_hash", columnList = "key_hash"),
        @Index(name = "idx_api_keys_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private UUID id;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "developer_id", nullable = false, length = 36)
    private String developerId;

    @Column(name = "subscription_id", nullable = false, length = 36)
    private String subscriptionId;

    @Column(name = "api_id", nullable = false, length = 36)
    private String apiId;

    @Column(name = "key_prefix", nullable = false, length = 32)
    private String keyPrefix;

    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    private String keyHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private KeyStatus status = KeyStatus.ACTIVE;

    @Column(name = "rate_limit_rps", nullable = false)
    @Builder.Default
    private int rateLimitRps = 10;

    @Column(name = "quota_monthly", nullable = false)
    @Builder.Default
    private int quotaMonthly = 10000;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
