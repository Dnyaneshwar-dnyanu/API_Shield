package com.apishield.payment.gateway;

import com.apishield.common.util.CryptoUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RazorpaySignatureVerificationTest {

    private RazorpayPaymentGatewayImpl gateway;
    private final String keyId = "rzp_test_mock_key";
    private final String keySecret = "secret_checkout_key_123";
    private final String webhookSecret = "secret_webhook_key_456";

    @BeforeEach
    void setUp() {
        gateway = new RazorpayPaymentGatewayImpl(keyId, keySecret, webhookSecret, true, new ObjectMapper());
    }

    @Test
    @DisplayName("Should successfully verify valid client checkout HMAC-SHA256 signature")
    void shouldVerifyValidClientSignature() {
        String orderId = "order_EKwxwAgItmmXdp";
        String paymentId = "pay_29MoEsz4dOFQDu";
        String expectedPayload = orderId + "|" + paymentId;
        String validSignature = CryptoUtils.hmacSha256Hex(expectedPayload, keySecret);

        boolean result = gateway.verifySignature(orderId, paymentId, validSignature);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject tampered client signature or mismatched payment ID")
    void shouldRejectInvalidClientSignature() {
        String orderId = "order_EKwxwAgItmmXdp";
        String paymentId = "pay_29MoEsz4dOFQDu";
        String validSignature = CryptoUtils.hmacSha256Hex(orderId + "|" + paymentId, keySecret);

        // Mismatched payment ID
        assertThat(gateway.verifySignature(orderId, "pay_TAMPERED", validSignature)).isFalse();
        // Mismatched order ID
        assertThat(gateway.verifySignature("order_TAMPERED", paymentId, validSignature)).isFalse();
        // Malformed signature
        assertThat(gateway.verifySignature(orderId, paymentId, "invalid_sig")).isFalse();
        // Null inputs
        assertThat(gateway.verifySignature(null, paymentId, validSignature)).isFalse();
        assertThat(gateway.verifySignature(orderId, null, validSignature)).isFalse();
        assertThat(gateway.verifySignature(orderId, paymentId, null)).isFalse();
    }

    @Test
    @DisplayName("Should successfully verify authentic webhook signature")
    void shouldVerifyValidWebhookSignature() {
        String webhookPayload = "{\"entity\":\"event\",\"event\":\"order.paid\"}";
        String validSignature = CryptoUtils.hmacSha256Hex(webhookPayload, webhookSecret);

        boolean result = gateway.verifyWebhookSignature(webhookPayload, validSignature);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject tampered webhook payload or mismatched secret")
    void shouldRejectTamperedWebhook() {
        String webhookPayload = "{\"entity\":\"event\",\"event\":\"order.paid\"}";
        String validSignature = CryptoUtils.hmacSha256Hex(webhookPayload, webhookSecret);

        // Tampered payload
        String tamperedPayload = "{\"entity\":\"event\",\"event\":\"order.paid\",\"tampered\":true}";
        assertThat(gateway.verifyWebhookSignature(tamperedPayload, validSignature)).isFalse();
        // Null checks
        assertThat(gateway.verifyWebhookSignature(null, validSignature)).isFalse();
        assertThat(gateway.verifyWebhookSignature(webhookPayload, null)).isFalse();
    }

    @Test
    @DisplayName("Should create mock order in mock mode")
    void shouldCreateMockOrder() {
        PaymentGatewayService.GatewayOrder order = gateway.createOrder(
                5000,
                "INR",
                "receipt_123",
                Map.of("subscriptionId", "sub-1")
        );

        assertThat(order).isNotNull();
        assertThat(order.orderId()).startsWith("order_rzp_mock_");
        assertThat(order.amountCents()).isEqualTo(5000);
        assertThat(order.currency()).isEqualTo("INR");
    }
}
