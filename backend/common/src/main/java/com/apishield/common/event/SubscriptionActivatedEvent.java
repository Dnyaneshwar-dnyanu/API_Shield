package com.apishield.common.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

/**
 * Domain event payload published when a developer's subscription is activated.
 */
@Getter
@ToString
@Builder
public class SubscriptionActivatedEvent {

    @JsonProperty("subscriptionId")
    private final String subscriptionId;

    @JsonProperty("developerId")
    private final String developerId;

    @JsonProperty("apiId")
    private final String apiId;

    @JsonProperty("planId")
    private final String planId;

    @JsonProperty("status")
    private final String status;

    @JsonProperty("rateLimitRps")
    private final int rateLimitRps;

    @JsonProperty("quotaMonthly")
    private final int quotaMonthly;

    @JsonProperty("activatedAt")
    private final Instant activatedAt;

    @JsonCreator
    public SubscriptionActivatedEvent(
            @JsonProperty("subscriptionId") String subscriptionId,
            @JsonProperty("developerId") String developerId,
            @JsonProperty("apiId") String apiId,
            @JsonProperty("planId") String planId,
            @JsonProperty("status") String status,
            @JsonProperty("rateLimitRps") int rateLimitRps,
            @JsonProperty("quotaMonthly") int quotaMonthly,
            @JsonProperty("activatedAt") Instant activatedAt) {
        this.subscriptionId = subscriptionId;
        this.developerId = developerId;
        this.apiId = apiId;
        this.planId = planId;
        this.status = status;
        this.rateLimitRps = rateLimitRps;
        this.quotaMonthly = quotaMonthly;
        this.activatedAt = activatedAt != null ? activatedAt : Instant.now();
    }
}
