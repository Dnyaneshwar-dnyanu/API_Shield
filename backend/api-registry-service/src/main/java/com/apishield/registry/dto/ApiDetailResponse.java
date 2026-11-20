package com.apishield.registry.dto;

import com.apishield.registry.model.ApiStatus;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiDetailResponse {

    private UUID id;
    private String ownerId;
    private String name;
    private String slug;
    private String description;
    private String category;
    private String targetBackendUrl;
    private ApiStatus status;
    private String versionLabel;
    private boolean isPublic;
    private Instant createdAt;
    private Instant updatedAt;
    private List<EndpointResponse> endpoints;
    private List<PlanResponse> plans;
}
