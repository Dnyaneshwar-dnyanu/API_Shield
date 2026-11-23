package com.apishield.notification.consumer;

import com.apishield.common.event.EventEnvelope;
import com.apishield.notification.entity.NotificationChannel;
import com.apishield.notification.entity.NotificationTemplateKey;
import com.apishield.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

class PaymentEventConsumerTest {

    private NotificationService notificationService;
    private ObjectMapper objectMapper;
    private PaymentEventConsumer consumer;

    @BeforeEach
    void setUp() {
        notificationService = Mockito.mock(NotificationService.class);
        objectMapper = new ObjectMapper();
        consumer = new PaymentEventConsumer(notificationService, objectMapper);
    }

    @Test
    @DisplayName("Should dispatch payment receipt on payment.completed event")
    void shouldDispatchReceiptOnPaymentCompleted() throws Exception {
        Map<String, Object> payload = Map.of(
                "developerId", "dev-200",
                "paymentId", "pay-300",
                "subscriptionId", "sub-400",
                "amountCents", 2999,
                "currency", "USD",
                "transactionReference", "tx-500"
        );

        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of(
                "payment.completed",
                "payment-service",
                null,
                "sub-400",
                payload
        );

        String json = objectMapper.writeValueAsString(envelope);
        consumer.handlePaymentEvents(json);

        verify(notificationService).createAndSend(
                eq("dev-200"),
                isNull(),
                eq(NotificationChannel.EMAIL),
                eq(NotificationTemplateKey.PAYMENT_SUCCESS),
                contains("Payment Confirmation"),
                anyMap()
        );
    }

    @Test
    @DisplayName("Should dispatch payment failed notice on payment.failed event")
    void shouldDispatchAlertOnPaymentFailed() throws Exception {
        Map<String, Object> payload = Map.of(
                "developerId", "dev-200",
                "subscriptionId", "sub-400",
                "reason", "Insufficient funds"
        );

        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of(
                "payment.failed",
                "payment-service",
                null,
                "sub-400",
                payload
        );

        String json = objectMapper.writeValueAsString(envelope);
        consumer.handlePaymentEvents(json);

        verify(notificationService).createAndSend(
                eq("dev-200"),
                isNull(),
                eq(NotificationChannel.EMAIL),
                eq(NotificationTemplateKey.PAYMENT_FAILED),
                contains("Payment Failed"),
                anyMap()
        );
    }
}
