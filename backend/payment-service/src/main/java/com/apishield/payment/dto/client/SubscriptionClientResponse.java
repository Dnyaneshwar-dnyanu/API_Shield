package com.apishield.payment.dto.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionClientResponse {
    private UUID id;
    private String developerId;
    private String apiId;
    private String planId;
    private String status;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
}
