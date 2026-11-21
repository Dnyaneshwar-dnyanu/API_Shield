package com.apishield.subscription.dto;

import com.apishield.subscription.model.SubscriptionStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionResponse {

    private UUID id;
    private String developerId;
    private String apiId;
    private String planId;
    private SubscriptionStatus status;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
    private Instant cancelledAt;
    private Instant createdAt;
}
