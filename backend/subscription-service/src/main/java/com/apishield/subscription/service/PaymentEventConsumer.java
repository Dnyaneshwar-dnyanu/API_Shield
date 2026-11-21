package com.apishield.subscription.service;

import com.apishield.common.constant.KafkaTopics;
import com.apishield.common.event.EventEnvelope;
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

    private final SubscriptionService subscriptionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.PAYMENT_EVENTS, groupId = "subscription-service-group")
    public void handlePaymentEvents(String message) {
        try {
            log.info("Received payment event message: {}", message);
            EventEnvelope<?> envelope = objectMapper.readValue(message, EventEnvelope.class);
            String eventType = envelope.getEventType();

            if ("payment.completed".equalsIgnoreCase(eventType)) {
                handlePaymentCompleted(envelope);
            } else if ("payment.failed".equalsIgnoreCase(eventType)) {
                handlePaymentFailed(envelope);
            } else {
                log.debug("Ignoring irrelevant payment event type: {}", eventType);
            }
        } catch (Exception e) {
            log.warn("Error processing payment event: {}", e.getMessage());
        }
    }

    private void handlePaymentCompleted(EventEnvelope<?> envelope) {
        try {
            Object payload = envelope.getPayload();
            Map<?, ?> map = objectMapper.convertValue(payload, Map.class);
            String subscriptionId = (String) map.get("subscriptionId");
            if (subscriptionId != null && !subscriptionId.isBlank()) {
                log.info("Processing payment.completed for subscription: {}", subscriptionId);
                subscriptionService.activateSubscriptionFromPayment(subscriptionId);
            }
        } catch (Exception e) {
            log.error("Failed to handle payment.completed event: {}", e.getMessage());
        }
    }

    private void handlePaymentFailed(EventEnvelope<?> envelope) {
        try {
            Object payload = envelope.getPayload();
            Map<?, ?> map = objectMapper.convertValue(payload, Map.class);
            String subscriptionId = (String) map.get("subscriptionId");
            if (subscriptionId != null && !subscriptionId.isBlank()) {
                log.info("Processing payment.failed for subscription: {}", subscriptionId);
                subscriptionService.suspendSubscriptionFromPayment(subscriptionId);
            }
        } catch (Exception e) {
            log.error("Failed to handle payment.failed event: {}", e.getMessage());
        }
    }
}
