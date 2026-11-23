package com.apishield.notification.service;

import com.apishield.notification.dto.DirectNotificationRequest;
import com.apishield.notification.dto.NotificationResponse;
import com.apishield.notification.entity.Notification;
import com.apishield.notification.entity.NotificationChannel;
import com.apishield.notification.entity.NotificationTemplateKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface NotificationService {

    NotificationResponse sendDirect(DirectNotificationRequest request);

    Notification createAndSend(
            String recipientUserId,
            String recipientEmail,
            NotificationChannel channel,
            NotificationTemplateKey templateKey,
            String title,
            Map<String, Object> payload
    );

    Page<NotificationResponse> getUserNotifications(String userId, boolean unreadOnly, Pageable pageable);

    NotificationResponse markAsRead(UUID notificationId, String userId);

    long getUnreadCount(String userId);
}
