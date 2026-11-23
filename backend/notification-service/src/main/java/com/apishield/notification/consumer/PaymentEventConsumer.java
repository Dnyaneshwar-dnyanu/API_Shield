package com.apishield.notification.consumer;

import com.apishield.common.constant.KafkaTopics;
import com.apishield.common.event.EventEnvelope;
import com.apishield.notification.entity.NotificationChannel;
import com.apishield.notification.entity.NotificationTemplateKey;
import com.apishield.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.PAYMENT_EVENTS, groupId = "notification-service-group")
    public void handlePaymentEvents(String message) {
        try {
            log.info("Received payment event message: {}", message);
            EventEnvelope<?> envelope = objectMapper.readValue(message, EventEnvelope.class);
            String eventType = envelope.getEventType();

            if ("payment.completed".equalsIgnoreCase(eventType) || "PAYMENT_COMPLETED".equalsIgnoreCase(eventType)) {
                handlePaymentCompleted(envelope);
            } else if ("payment.failed".equalsIgnoreCase(eventType) || "PAYMENT_FAILED".equalsIgnoreCase(eventType)) {
                handlePaymentFailed(envelope);
            } else {
                log.debug("Ignoring non-actionable payment event type: {}", eventType);
            }
        } catch (Exception e) {
            log.warn("Error processing payment event in notification-service: {}", e.getMessage());
        }
    }

    private void handlePaymentCompleted(EventEnvelope<?> envelope) {
        try {
            Object payload = envelope.getPayload();
            Map<?, ?> map = objectMapper.convertValue(payload, Map.class);
            String developerId = (String) map.get("developerId");
            String paymentId = (String) map.get("paymentId");
            String subscriptionId = (String) map.get("subscriptionId");
            Number amountCents = (Number) map.get("amountCents");
            String currency = (String) map.get("currency");
            String txRef = (String) map.get("transactionReference");

            double amount = amountCents != null ? amountCents.doubleValue() / 100.0 : 0.0;

            if (developerId != null) {
                log.info("Dispatching payment receipt notification for developer: {}, payment: {}", developerId, paymentId);
                notificationService.createAndSend(
                        developerId,
                        null,
                        NotificationChannel.EMAIL,
                        NotificationTemplateKey.PAYMENT_SUCCESS,
                        "APIShield — Payment Confirmation",
                        Map.of(
                                "paymentId", paymentId != null ? paymentId : "",
                                "subscriptionId", subscriptionId != null ? subscriptionId : "",
                                "amount", String.format("%.2f", amount),
                                "currency", currency != null ? currency : "USD",
                                "transactionReference", txRef != null ? txRef : ""
                        )
                );
            }
        } catch (Exception e) {
            log.error("Failed to process payment.completed notification: {}", e.getMessage());
        }
    }

    private void handlePaymentFailed(EventEnvelope<?> envelope) {
        try {
            Object payload = envelope.getPayload();
            Map<?, ?> map = objectMapper.convertValue(payload, Map.class);
            String developerId = (String) map.get("developerId");
            String subscriptionId = (String) map.get("subscriptionId");
            String reason = (String) map.get("reason");

            if (developerId != null) {
                log.info("Dispatching payment failed notification for developer: {}, subscription: {}", developerId, subscriptionId);
                notificationService.createAndSend(
                        developerId,
                        null,
                        NotificationChannel.EMAIL,
                        NotificationTemplateKey.PAYMENT_FAILED,
                        "APIShield — Payment Failed Alert",
                        Map.of(
                                "subscriptionId", subscriptionId != null ? subscriptionId : "",
                                "reason", reason != null ? reason : "Card declined or expired"
                        )
                );
            }
        } catch (Exception e) {
            log.error("Failed to process payment.failed notification: {}", e.getMessage());
        }
    }
}
