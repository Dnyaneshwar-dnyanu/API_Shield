package com.apishield.registry.dto;

import com.apishield.registry.model.BillingCycle;
import com.apishield.registry.model.PlanType;
import lombok.*;

import java.time.Instant;
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
    private PlanType planType;
    private int priceCents;
    private BillingCycle billingCycle;
    private int rateLimitRps;
    private int quotaMonthly;
    private boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
