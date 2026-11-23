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
public class UserEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.USER_EVENTS, groupId = "notification-service-group")
    public void handleUserEvents(String message) {
        try {
            log.info("Received user event message: {}", message);
            EventEnvelope<?> envelope = objectMapper.readValue(message, EventEnvelope.class);
            String eventType = envelope.getEventType();

            if ("user.registered".equalsIgnoreCase(eventType) || "USER_REGISTERED".equalsIgnoreCase(eventType)) {
                handleUserRegistered(envelope);
            } else {
                log.debug("Ignoring non-actionable user event type: {}", eventType);
            }
        } catch (Exception e) {
            log.warn("Error processing user event in notification-service: {}", e.getMessage());
        }
    }

    private void handleUserRegistered(EventEnvelope<?> envelope) {
        try {
            Object payload = envelope.getPayload();
            Map<?, ?> map = objectMapper.convertValue(payload, Map.class);
            String userId = (String) map.get("userId");
            String email = (String) map.get("email");
            String firstName = (String) map.get("firstName");
            String role = (String) map.get("role");

            if (userId != null && email != null) {
                log.info("Dispatching welcome email to new user: {} ({})", userId, email);
                notificationService.createAndSend(
                        userId,
                        email,
                        NotificationChannel.EMAIL,
                        NotificationTemplateKey.WELCOME_EMAIL,
                        "Welcome to APIShield!",
                        Map.of(
                                "name", firstName != null ? firstName : "Developer",
                                "email", email,
                                "role", role != null ? role : "DEVELOPER"
                        )
                );
            }
        } catch (Exception e) {
            log.error("Failed to process user.registered notification: {}", e.getMessage());
        }
    }
}
