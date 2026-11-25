package com.apishield.registry.dto;

import com.apishield.registry.model.BillingCycle;
import com.apishield.registry.model.PlanType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePlanRequest {

    @NotBlank(message = "Plan name is required")
    @Size(max = 50, message = "Plan name must not exceed 50 characters")
    private String name;

    @NotNull(message = "Plan type is required")
    @JsonAlias({"tier", "plan_type"})
    @Builder.Default
    private PlanType planType = PlanType.FREE;

    @Min(value = 0, message = "Price in cents must be zero or positive")
    @JsonAlias({"priceCents", "price_cents", "monthlyPrice", "monthly_price"})
    @Builder.Default
    private Integer priceCents = 0;

    @NotNull(message = "Billing cycle is required")
    @JsonAlias({"billingCycle", "billing_cycle"})
    @Builder.Default
    private BillingCycle billingCycle = BillingCycle.MONTHLY;

    @Min(value = 1, message = "Rate limit RPS must be at least 1")
    @JsonAlias({"rateLimitRps", "rate_limit_rps", "rps"})
    @Builder.Default
    private Integer rateLimitRps = 10;

    @Min(value = 100, message = "Monthly quota must be at least 100")
    @JsonAlias({"quotaMonthly", "quota_monthly", "monthlyQuota", "monthly_quota"})
    @Builder.Default
    private Integer quotaMonthly = 10000;

    @JsonProperty("isActive")
    @Builder.Default
    private Boolean isActive = true;

    public int getPriceCents() {
        return priceCents != null ? priceCents : 0;
    }

    public int getRateLimitRps() {
        return rateLimitRps != null ? rateLimitRps : 10;
    }

    public int getQuotaMonthly() {
        return quotaMonthly != null ? quotaMonthly : 10000;
    }

    public Boolean isActive() {
        return isActive != null ? isActive : true;
    }
}
