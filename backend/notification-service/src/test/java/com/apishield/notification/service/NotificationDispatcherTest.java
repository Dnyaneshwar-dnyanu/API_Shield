package com.apishield.notification.service;

import com.apishield.notification.config.NotificationProperties;
import com.apishield.notification.entity.Notification;
import com.apishield.notification.entity.NotificationChannel;
import com.apishield.notification.entity.NotificationStatus;
import com.apishield.notification.entity.NotificationTemplateKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationDispatcherTest {

    private NotificationProperties properties;
    private NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        properties = new NotificationProperties();
        properties.setMockDelivery(true);
        properties.setFromEmail("no-reply@apishield.io");
        dispatcher = new NotificationDispatcher(properties);
    }

    @Test
    @DisplayName("Should successfully dispatch mock email when recipient email is provided")
    void shouldDispatchMockEmail() {
        Notification notification = Notification.builder()
                .recipientUserId("user-001")
                .recipientEmail("developer@example.com")
                .channel(NotificationChannel.EMAIL)
                .templateKey(NotificationTemplateKey.WELCOME_EMAIL)
                .title("Welcome")
                .contentPayload("{}")
                .status(NotificationStatus.PENDING)
                .build();

        dispatcher.dispatch(notification, "Hello Developer!");

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getSentAt()).isNotNull();
    }

    @Test
    @DisplayName("Should fail email dispatch when recipient email is missing")
    void shouldFailWhenRecipientEmailMissing() {
        Notification notification = Notification.builder()
                .recipientUserId("user-002")
                .recipientEmail(null)
                .channel(NotificationChannel.EMAIL)
                .templateKey(NotificationTemplateKey.WELCOME_EMAIL)
                .title("Welcome")
                .contentPayload("{}")
                .status(NotificationStatus.PENDING)
                .build();

        dispatcher.dispatch(notification, "Hello!");

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getErrorMessage()).contains("missing");
    }

    @Test
    @DisplayName("Should mark IN_APP notification as ready and sent")
    void shouldDispatchInAppNotification() {
        Notification notification = Notification.builder()
                .recipientUserId("user-003")
                .channel(NotificationChannel.IN_APP)
                .templateKey(NotificationTemplateKey.DIRECT_ALERT)
                .title("Alert")
                .contentPayload("{}")
                .status(NotificationStatus.PENDING)
                .build();

        dispatcher.dispatch(notification, "New announcement");

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getSentAt()).isNotNull();
    }
}
