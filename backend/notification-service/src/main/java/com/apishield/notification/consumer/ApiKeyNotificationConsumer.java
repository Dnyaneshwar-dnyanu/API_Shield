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
public class ApiKeyNotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.APIKEY_EVENTS, groupId = "notification-service-group")
    public void handleApiKeyEvents(String message) {
        try {
            log.info("Received apikey event message: {}", message);
            EventEnvelope<?> envelope = objectMapper.readValue(message, EventEnvelope.class);
            String eventType = envelope.getEventType();

            if ("apikey.revoked".equalsIgnoreCase(eventType) || "APIKEY_REVOKED".equalsIgnoreCase(eventType)) {
                handleApiKeyRevoked(envelope);
            } else {
                log.debug("Ignoring non-actionable apikey event type: {}", eventType);
            }
        } catch (Exception e) {
            log.warn("Error processing apikey event in notification-service: {}", e.getMessage());
        }
    }

    private void handleApiKeyRevoked(EventEnvelope<?> envelope) {
        try {
            Object payload = envelope.getPayload();
            Map<?, ?> map = objectMapper.convertValue(payload, Map.class);
            String developerId = (String) map.get("developerId");
            String apiKeyId = (String) map.get("apiKeyId");
            String reason = (String) map.get("reason");

            if (developerId != null) {
                log.info("Dispatching apikey revoked security notice for developer: {}, key: {}", developerId, apiKeyId);
                notificationService.createAndSend(
                        developerId,
                        null,
                        NotificationChannel.EMAIL,
                        NotificationTemplateKey.KEY_REVOKED,
                        "SECURITY ALERT: API Key Revoked",
                        Map.of(
                                "apiKeyId", apiKeyId != null ? apiKeyId : "",
                                "reason", reason != null ? reason : "Key revoked by developer or subscription cancellation"
                        )
                );
            }
        } catch (Exception e) {
            log.error("Failed to process apikey.revoked notification: {}", e.getMessage());
        }
    }
}
