package com.apishield.registry.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogItemResponse {

    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String category;
    private String versionLabel;
    private int endpointCount;
    private int planCount;
}
