package com.apishield.registry.dto;

import com.apishield.registry.model.HttpMethodType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointResponse {

    private UUID id;
    private UUID apiId;
    private String pathPattern;
    private HttpMethodType httpMethod;
    private String description;
    private boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
