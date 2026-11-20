package com.apishield.common.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

/**
 * Domain event payload published when a billing payment succeeds.
 */
@Getter
@ToString
@Builder
public class PaymentCompletedEvent {

    @JsonProperty("paymentId")
    private final String paymentId;

    @JsonProperty("subscriptionId")
    private final String subscriptionId;

    @JsonProperty("developerId")
    private final String developerId;

    @JsonProperty("amountCents")
    private final int amountCents;

    @JsonProperty("currency")
    private final String currency;

    @JsonProperty("provider")
    private final String provider;

    @JsonProperty("transactionReference")
    private final String transactionReference;

    @JsonProperty("completedAt")
    private final Instant completedAt;

    @JsonCreator
    public PaymentCompletedEvent(
            @JsonProperty("paymentId") String paymentId,
            @JsonProperty("subscriptionId") String subscriptionId,
            @JsonProperty("developerId") String developerId,
            @JsonProperty("amountCents") int amountCents,
            @JsonProperty("currency") String currency,
            @JsonProperty("provider") String provider,
            @JsonProperty("transactionReference") String transactionReference,
            @JsonProperty("completedAt") Instant completedAt) {
        this.paymentId = paymentId;
        this.subscriptionId = subscriptionId;
        this.developerId = developerId;
        this.amountCents = amountCents;
        this.currency = currency;
        this.provider = provider;
        this.transactionReference = transactionReference;
        this.completedAt = completedAt != null ? completedAt : Instant.now();
    }
}
