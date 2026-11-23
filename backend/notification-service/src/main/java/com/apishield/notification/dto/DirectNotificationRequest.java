package com.apishield.notification.dto;

import com.apishield.notification.entity.NotificationChannel;
import com.apishield.notification.entity.NotificationTemplateKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectNotificationRequest {

    @NotBlank(message = "Recipient user ID is required")
    private String recipientUserId;

    private String recipientEmail;

    @Builder.Default
    private NotificationChannel channel = NotificationChannel.EMAIL;

    @NotNull(message = "Template key is required")
    private NotificationTemplateKey templateKey;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content payload is required")
    private String contentPayload;
}
