package com.apishield.subscription.dto;

import com.apishield.subscription.model.SubscriptionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionValidationResponse {

    private UUID subscriptionId;
    private String developerId;
    private String apiId;
    private String planId;
    private SubscriptionStatus status;
    private boolean active;
    private int rateLimitRps;
    private int quotaMonthly;
    private Instant currentPeriodEnd;

    @JsonProperty("valid")
    public boolean isValid() {
        return active;
    }
}
