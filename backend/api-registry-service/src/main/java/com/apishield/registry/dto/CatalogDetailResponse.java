package com.apishield.registry.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogDetailResponse {

    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String category;
    private String versionLabel;
    private List<EndpointResponse> endpoints;
    private List<PlanResponse> plans;
}
