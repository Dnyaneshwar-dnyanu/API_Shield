package com.apishield.registry.entity;

import com.apishield.registry.model.ApiStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "apis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Api {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", updatable = false, nullable = false, length = 36)
    private UUID id;

    @Column(name = "owner_id", nullable = false, length = 36)
    private String ownerId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "target_backend_url", nullable = false, length = 500)
    private String targetBackendUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ApiStatus status = ApiStatus.DRAFT;

    @Column(name = "version_label", nullable = false, length = 20)
    @Builder.Default
    private String versionLabel = "v1";

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean isPublic = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(mappedBy = "api", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ApiEndpoint> endpoints = new ArrayList<>();

    @OneToMany(mappedBy = "api", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ApiPlan> plans = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.status == null) {
            this.status = ApiStatus.DRAFT;
        }
        if (this.versionLabel == null) {
            this.versionLabel = "v1";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addEndpoint(ApiEndpoint endpoint) {
        endpoints.add(endpoint);
        endpoint.setApi(this);
    }

    public void addPlan(ApiPlan plan) {
        plans.add(plan);
        plan.setApi(this);
    }
}
