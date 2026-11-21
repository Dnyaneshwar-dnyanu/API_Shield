package com.apishield.subscription.service;

import com.apishield.common.event.EventEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.mockito.Mockito.verify;

class PaymentEventConsumerTest {

    private SubscriptionService subscriptionService;
    private ObjectMapper objectMapper;
    private PaymentEventConsumer consumer;

    @BeforeEach
    void setUp() {
        subscriptionService = Mockito.mock(SubscriptionService.class);
        objectMapper = new ObjectMapper();
        consumer = new PaymentEventConsumer(subscriptionService, objectMapper);
    }

    @Test
    @DisplayName("Should invoke subscription activation on payment.completed event")
    void shouldActivateSubscriptionOnPaymentCompleted() throws Exception {
        String subId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
        Map<String, Object> payload = Map.of(
                "paymentId", "pay_123456",
                "subscriptionId", subId,
                "developerId", "dev_001",
                "amountCents", 4900
        );

        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of(
                "payment.completed",
                "payment-service",
                null,
                subId,
                payload
        );

        String json = objectMapper.writeValueAsString(envelope);
        consumer.handlePaymentEvents(json);

        verify(subscriptionService).activateSubscriptionFromPayment(subId);
    }

    @Test
    @DisplayName("Should invoke subscription suspension on payment.failed event")
    void shouldSuspendSubscriptionOnPaymentFailed() throws Exception {
        String subId = "b2c3d4e5-f6a7-8901-bcde-f12345678901";
        Map<String, Object> payload = Map.of(
                "paymentId", "pay_789012",
                "subscriptionId", subId,
                "reason", "Insufficient funds"
        );

        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of(
                "payment.failed",
                "payment-service",
                null,
                subId,
                payload
        );

        String json = objectMapper.writeValueAsString(envelope);
        consumer.handlePaymentEvents(json);

        verify(subscriptionService).suspendSubscriptionFromPayment(subId);
    }
}
