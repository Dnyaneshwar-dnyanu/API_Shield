package com.apishield.payment.dto;

import com.apishield.payment.model.InvoiceStatus;
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
public class InvoiceResponse {
    private UUID id;
    private String paymentId;
    private String developerId;
    private String invoiceNumber;
    private int amountCents;
    private String currency;
    private InvoiceStatus status;
    private Instant billingPeriodStart;
    private Instant billingPeriodEnd;
    private Instant createdAt;
}
