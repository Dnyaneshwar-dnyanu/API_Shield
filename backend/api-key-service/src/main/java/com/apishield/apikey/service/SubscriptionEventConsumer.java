package com.apishield.apikey.service;

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
public class SubscriptionEventConsumer {

    private final ApiKeyService apiKeyService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.SUBSCRIPTION_EVENTS, groupId = "api-key-service-group")
    public void handleSubscriptionEvents(String message) {
        try {
            log.info("Received subscription event message: {}", message);
            EventEnvelope<?> envelope = objectMapper.readValue(message, EventEnvelope.class);
            String eventType = envelope.getEventType();

            if ("subscription.cancelled".equalsIgnoreCase(eventType)) {
                handleSubscriptionCancelled(envelope);
            } else {
                log.debug("Ignoring irrelevant subscription event type: {}", eventType);
            }
        } catch (Exception e) {
            log.warn("Error processing subscription event in api-key-service: {}", e.getMessage());
        }
    }

    private void handleSubscriptionCancelled(EventEnvelope<?> envelope) {
        try {
            Object payload = envelope.getPayload();
            Map<?, ?> map = objectMapper.convertValue(payload, Map.class);
            String subscriptionId = (String) map.get("subscriptionId");
            if (subscriptionId != null && !subscriptionId.isBlank()) {
                log.info("Handling subscription.cancelled for subscription: {}. Revoking active keys.", subscriptionId);
                apiKeyService.revokeKeysBySubscriptionId(subscriptionId, "Subscription cancelled");
            }
        } catch (Exception e) {
            log.error("Failed to revoke keys for cancelled subscription: {}", e.getMessage());
        }
    }
}
