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

class UserEventConsumerTest {

    private NotificationService notificationService;
    private ObjectMapper objectMapper;
    private UserEventConsumer consumer;

    @BeforeEach
    void setUp() {
        notificationService = Mockito.mock(NotificationService.class);
        objectMapper = new ObjectMapper();
        consumer = new UserEventConsumer(notificationService, objectMapper);
    }

    @Test
    @DisplayName("Should dispatch welcome email on user.registered event")
    void shouldDispatchWelcomeEmailOnUserRegistered() throws Exception {
        Map<String, Object> payload = Map.of(
                "userId", "usr-100",
                "email", "developer@example.com",
                "firstName", "John",
                "role", "DEVELOPER"
        );

        EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of(
                "user.registered",
                "user-service",
                null,
                "usr-100",
                payload
        );

        String json = objectMapper.writeValueAsString(envelope);
        consumer.handleUserEvents(json);

        verify(notificationService).createAndSend(
                eq("usr-100"),
                eq("developer@example.com"),
                eq(NotificationChannel.EMAIL),
                eq(NotificationTemplateKey.WELCOME_EMAIL),
                contains("Welcome"),
                anyMap()
        );
    }
}
