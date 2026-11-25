package com.apishield.subscription.dto.client;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanResponse {

    private UUID id;
    private UUID apiId;
    private String name;
    private String planType;
    private int priceCents;
    private String billingCycle;
    private int rateLimitRps;
    private int quotaMonthly;

    @JsonProperty("isActive")
    @JsonAlias({"active", "isActive"})
    private boolean isActive;

    private Instant createdAt;
    private Instant updatedAt;
}
