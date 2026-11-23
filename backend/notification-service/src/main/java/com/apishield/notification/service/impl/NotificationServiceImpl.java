package com.apishield.notification.service.impl;

import com.apishield.notification.dto.DirectNotificationRequest;
import com.apishield.notification.dto.NotificationResponse;
import com.apishield.notification.entity.Notification;
import com.apishield.notification.entity.NotificationChannel;
import com.apishield.notification.entity.NotificationStatus;
import com.apishield.notification.entity.NotificationTemplateKey;
import com.apishield.notification.repository.NotificationRepository;
import com.apishield.notification.service.NotificationDispatcher;
import com.apishield.notification.service.NotificationService;
import com.apishield.notification.service.NotificationTemplateEngine;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateEngine templateEngine;
    private final NotificationDispatcher dispatcher;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public NotificationResponse sendDirect(DirectNotificationRequest request) {
        Map<String, Object> payloadMap;
        try {
            payloadMap = objectMapper.readValue(request.getContentPayload(), new TypeReference<>() {});
        } catch (Exception e) {
            payloadMap = Map.of("message", request.getContentPayload());
        }

        Notification notification = createAndSend(
                request.getRecipientUserId(),
                request.getRecipientEmail(),
                request.getChannel() != null ? request.getChannel() : NotificationChannel.EMAIL,
                request.getTemplateKey(),
                request.getTitle(),
                payloadMap
        );

        return NotificationResponse.fromEntity(notification);
    }

    @Override
    @Transactional
    public Notification createAndSend(
            String recipientUserId,
            String recipientEmail,
            NotificationChannel channel,
            NotificationTemplateKey templateKey,
            String title,
            Map<String, Object> payload
    ) {
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            jsonPayload = "{}";
        }

        Notification notification = Notification.builder()
                .recipientUserId(recipientUserId)
                .recipientEmail(recipientEmail)
                .channel(channel != null ? channel : NotificationChannel.EMAIL)
                .templateKey(templateKey)
                .title(title)
                .contentPayload(jsonPayload)
                .status(NotificationStatus.PENDING)
                .isRead(false)
                .retryCount(0)
                .build();

        notification = notificationRepository.save(notification);

        String renderedBody = templateEngine.renderEmailBody(templateKey, payload);
        dispatcher.dispatch(notification, renderedBody);

        return notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(String userId, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = unreadOnly
                ? notificationRepository.findByRecipientUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable);

        return page.map(NotificationResponse::fromEntity);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!notification.getRecipientUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot access notifications of another user");
        }

        notification.setRead(true);
        notification = notificationRepository.save(notification);
        return NotificationResponse.fromEntity(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByRecipientUserIdAndIsReadFalse(userId);
    }
}
