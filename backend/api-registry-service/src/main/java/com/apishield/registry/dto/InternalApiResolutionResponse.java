package com.apishield.registry.dto;

import com.apishield.registry.model.ApiStatus;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalApiResolutionResponse {

    private UUID apiId;
    private String slug;
    private String targetBackendUrl;
    private ApiStatus status;
    private List<EndpointResponse> endpoints;
    private List<PlanResponse> plans;
}
