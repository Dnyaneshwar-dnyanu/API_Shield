package com.apishield.payment.gateway;

import java.util.Map;

public interface PaymentGatewayService {

    record GatewayOrder(String orderId, int amountCents, String currency) {}

    GatewayOrder createOrder(int amountCents, String currency, String receipt, Map<String, String> notes);

    boolean verifySignature(String orderId, String paymentId, String signature);

    boolean verifyWebhookSignature(String rawPayload, String signature);

    String getKeyId();
}
