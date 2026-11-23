package com.apishield.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiMetadata {
    private UUID apiId;
    private String slug;
    private String targetBackendUrl;
    private String status;
}
