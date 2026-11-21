package com.apishield.payment.dto;

import com.apishield.payment.model.PaymentStatus;
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
public class PaymentResponse {
    private UUID id;
    private String subscriptionId;
    private String developerId;
    private String planId;
    private String provider;
    private String orderId;
    private String paymentId;
    private int amountCents;
    private String currency;
    private PaymentStatus status;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
}
