package com.apishield.notification.dto;

import com.apishield.notification.entity.Notification;
import com.apishield.notification.entity.NotificationChannel;
import com.apishield.notification.entity.NotificationStatus;
import com.apishield.notification.entity.NotificationTemplateKey;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private String recipientUserId;
    private String recipientEmail;
    private NotificationChannel channel;
    private NotificationTemplateKey templateKey;
    private String title;
    private String contentPayload;
    private NotificationStatus status;
    @JsonProperty("isRead")
    private boolean isRead;
    private Instant sentAt;
    private Instant createdAt;

    public static NotificationResponse fromEntity(Notification entity) {
        return NotificationResponse.builder()
                .id(entity.getId())
                .recipientUserId(entity.getRecipientUserId())
                .recipientEmail(entity.getRecipientEmail())
                .channel(entity.getChannel())
                .templateKey(entity.getTemplateKey())
                .title(entity.getTitle())
                .contentPayload(entity.getContentPayload())
                .status(entity.getStatus())
                .isRead(entity.isRead())
                .sentAt(entity.getSentAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
