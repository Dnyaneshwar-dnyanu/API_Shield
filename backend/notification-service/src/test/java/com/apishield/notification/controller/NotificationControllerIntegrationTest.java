package com.apishield.notification.controller;

import com.apishield.common.constant.HeaderConstants;
import com.apishield.notification.config.TestNotificationConfig;
import com.apishield.notification.dto.DirectNotificationRequest;
import com.apishield.notification.entity.Notification;
import com.apishield.notification.entity.NotificationChannel;
import com.apishield.notification.entity.NotificationStatus;
import com.apishield.notification.entity.NotificationTemplateKey;
import com.apishield.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestNotificationConfig.class)
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository repository;

    private static final String INTERNAL_TOKEN = "test-internal-secret-token-key-32bytes-min!";

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/notifications/send-direct should succeed with valid internal token")
    void shouldSendDirectNotificationWithValidInternalToken() throws Exception {
        DirectNotificationRequest request = DirectNotificationRequest.builder()
                .recipientUserId("dev-direct-1")
                .recipientEmail("dev-direct@apishield.io")
                .channel(NotificationChannel.EMAIL)
                .templateKey(NotificationTemplateKey.DIRECT_ALERT)
                .title("Scheduled Maintenance")
                .contentPayload("{\"message\":\"Platform update at 02:00 UTC\"}")
                .build();

        mockMvc.perform(post("/api/v1/notifications/send-direct")
                        .header(HeaderConstants.X_INTERNAL_TOKEN, INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipientUserId", is("dev-direct-1")))
                .andExpect(jsonPath("$.title", is("Scheduled Maintenance")))
                .andExpect(jsonPath("$.status", is("SENT")));

        assertThat(repository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("POST /api/v1/notifications/send-direct should fail with 401 when internal token is invalid")
    void shouldRejectDirectNotificationWithInvalidInternalToken() throws Exception {
        DirectNotificationRequest request = DirectNotificationRequest.builder()
                .recipientUserId("dev-direct-2")
                .recipientEmail("dev2@apishield.io")
                .channel(NotificationChannel.EMAIL)
                .templateKey(NotificationTemplateKey.DIRECT_ALERT)
                .title("Alert")
                .contentPayload("{}")
                .build();

        mockMvc.perform(post("/api/v1/notifications/send-direct")
                        .header(HeaderConstants.X_INTERNAL_TOKEN, "invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/notifications/my should return user's paginated notifications")
    void shouldReturnUserNotifications() throws Exception {
        String userId = "user-alice";

        Notification n1 = repository.save(Notification.builder()
                .recipientUserId(userId)
                .recipientEmail("alice@apishield.io")
                .channel(NotificationChannel.EMAIL)
                .templateKey(NotificationTemplateKey.WELCOME_EMAIL)
                .title("Welcome Alice")
                .contentPayload("{}")
                .status(NotificationStatus.SENT)
                .isRead(false)
                .build());

        Notification n2 = repository.save(Notification.builder()
                .recipientUserId(userId)
                .recipientEmail("alice@apishield.io")
                .channel(NotificationChannel.IN_APP)
                .templateKey(NotificationTemplateKey.DIRECT_ALERT)
                .title("Notice")
                .contentPayload("{}")
                .status(NotificationStatus.SENT)
                .isRead(true)
                .build());

        // GET all
        mockMvc.perform(get("/api/v1/notifications/my")
                        .header(HeaderConstants.X_USER_ID, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)));

        // GET unread only
        mockMvc.perform(get("/api/v1/notifications/my")
                        .header(HeaderConstants.X_USER_ID, userId)
                        .param("unreadOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].title", is("Welcome Alice")));
    }

    @Test
    @DisplayName("PUT /api/v1/notifications/{id}/read should mark notification as read")
    void shouldMarkNotificationAsRead() throws Exception {
        String userId = "user-bob";
        Notification notification = repository.save(Notification.builder()
                .recipientUserId(userId)
                .recipientEmail("bob@apishield.io")
                .channel(NotificationChannel.IN_APP)
                .templateKey(NotificationTemplateKey.DIRECT_ALERT)
                .title("Unread Alert")
                .contentPayload("{}")
                .status(NotificationStatus.SENT)
                .isRead(false)
                .build());

        mockMvc.perform(put("/api/v1/notifications/" + notification.getId() + "/read")
                        .header(HeaderConstants.X_USER_ID, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead", is(true)));

        Notification updated = repository.findById(notification.getId()).orElseThrow();
        assertThat(updated.isRead()).isTrue();
    }

    @Test
    @DisplayName("GET /api/v1/notifications/unread-count should return correct count")
    void shouldReturnUnreadCount() throws Exception {
        String userId = "user-carol";

        repository.save(Notification.builder()
                .recipientUserId(userId)
                .channel(NotificationChannel.IN_APP)
                .templateKey(NotificationTemplateKey.DIRECT_ALERT)
                .title("Alert 1")
                .contentPayload("{}")
                .isRead(false)
                .build());

        repository.save(Notification.builder()
                .recipientUserId(userId)
                .channel(NotificationChannel.IN_APP)
                .templateKey(NotificationTemplateKey.DIRECT_ALERT)
                .title("Alert 2")
                .contentPayload("{}")
                .isRead(false)
                .build());

        repository.save(Notification.builder()
                .recipientUserId(userId)
                .channel(NotificationChannel.IN_APP)
                .templateKey(NotificationTemplateKey.DIRECT_ALERT)
                .title("Alert 3 (Read)")
                .contentPayload("{}")
                .isRead(true)
                .build());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header(HeaderConstants.X_USER_ID, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount", is(2)));
    }
}
