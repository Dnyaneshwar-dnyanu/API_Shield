package com.apishield.subscription.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    private boolean isActive;
}
