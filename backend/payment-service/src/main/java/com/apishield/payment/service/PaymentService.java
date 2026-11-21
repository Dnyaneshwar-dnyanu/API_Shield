package com.apishield.payment.service;

import com.apishield.payment.dto.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PaymentService {

    CreateOrderResponse createOrder(CreateOrderRequest request, String developerId);

    PaymentResponse verifyPayment(VerifyPaymentRequest request, String developerId);

    Map<String, Object> processWebhook(String rawPayload, String signature);

    List<PaymentResponse> getHistory(String developerId);

    PaymentResponse getPaymentById(UUID id, String developerId);

    InvoiceResponse getInvoiceByPaymentId(UUID paymentId, String developerId);
}
