package com.apishield.payment.dto.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanClientResponse {
    private UUID id;
    private String name;
    private String planType;
    private Integer rateLimitRps;
    private Integer quotaMonthly;
    private int priceCents;
    private String billingCycle;
    private Boolean isActive;
}
