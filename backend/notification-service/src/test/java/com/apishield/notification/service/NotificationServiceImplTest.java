package com.apishield.notification.service;

import com.apishield.notification.dto.DirectNotificationRequest;
import com.apishield.notification.dto.NotificationResponse;
import com.apishield.notification.entity.Notification;
import com.apishield.notification.entity.NotificationChannel;
import com.apishield.notification.entity.NotificationStatus;
import com.apishield.notification.entity.NotificationTemplateKey;
import com.apishield.notification.repository.NotificationRepository;
import com.apishield.notification.service.impl.NotificationServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceImplTest {

    private NotificationRepository repository;
    private NotificationTemplateEngine templateEngine;
    private NotificationDispatcher dispatcher;
    private ObjectMapper objectMapper;
    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(NotificationRepository.class);
        templateEngine = Mockito.mock(NotificationTemplateEngine.class);
        dispatcher = Mockito.mock(NotificationDispatcher.class);
        objectMapper = new ObjectMapper();
        service = new NotificationServiceImpl(repository, templateEngine, dispatcher, objectMapper);
    }

    @Test
    @DisplayName("Should successfully send direct notification")
    void shouldSendDirectNotification() {
        UUID id = UUID.randomUUID();
        when(repository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            if (n.getId() == null) {
                n.setId(id);
            }
            return n;
        });
        when(templateEngine.renderEmailBody(any(), any())).thenReturn("Rendered message");

        DirectNotificationRequest request = DirectNotificationRequest.builder()
                .recipientUserId("dev-1")
                .recipientEmail("dev1@example.com")
                .channel(NotificationChannel.EMAIL)
                .templateKey(NotificationTemplateKey.DIRECT_ALERT)
                .title("Maintenance Alert")
                .contentPayload("{\"message\":\"System reboot at midnight\"}")
                .build();

        NotificationResponse response = service.sendDirect(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getRecipientUserId()).isEqualTo("dev-1");
        verify(dispatcher).dispatch(any(Notification.class), eq("Rendered message"));
    }

    @Test
    @DisplayName("Should mark notification as read for correct user")
    void shouldMarkAsRead() {
        UUID id = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(id)
                .recipientUserId("user-123")
                .channel(NotificationChannel.IN_APP)
                .templateKey(NotificationTemplateKey.DIRECT_ALERT)
                .title("Notice")
                .contentPayload("{}")
                .isRead(false)
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(notification));
        when(repository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse response = service.markAsRead(id, "user-123");

        assertThat(response.isRead()).isTrue();
        verify(repository).save(notification);
    }

    @Test
    @DisplayName("Should throw 403 Forbidden when marking notification of another user")
    void shouldThrowWhenMarkingAnotherUserNotification() {
        UUID id = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(id)
                .recipientUserId("user-owner")
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> service.markAsRead(id, "attacker-user"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot access notifications of another user");
    }

    @Test
    @DisplayName("Should return unread count")
    void shouldReturnUnreadCount() {
        when(repository.countByRecipientUserIdAndIsReadFalse("user-456")).thenReturn(5L);

        long count = service.getUnreadCount("user-456");

        assertThat(count).isEqualTo(5L);
        verify(repository).countByRecipientUserIdAndIsReadFalse("user-456");
    }
}
