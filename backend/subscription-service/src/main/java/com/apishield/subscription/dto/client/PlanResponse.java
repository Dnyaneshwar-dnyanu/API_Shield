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

    @Builder.Default
    private Integer priceCents = 0;

    private String billingCycle;

    @Builder.Default
    private Integer rateLimitRps = 10;

    @Builder.Default
    private Integer quotaMonthly = 10000;

    @JsonProperty("isActive")
    @JsonAlias({"active", "isActive"})
    @Builder.Default
    private Boolean isActive = true;

    private Instant createdAt;
    private Instant updatedAt;

    public int getPriceCents() {
        return priceCents != null ? priceCents : 0;
    }

    public int getRateLimitRps() {
        return rateLimitRps != null ? rateLimitRps : 10;
    }

    public int getQuotaMonthly() {
        return quotaMonthly != null ? quotaMonthly : 10000;
    }

    public boolean isActive() {
        return isActive != null ? isActive : true;
    }
}
