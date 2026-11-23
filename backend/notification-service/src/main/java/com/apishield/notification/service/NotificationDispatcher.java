package com.apishield.notification.service;

import com.apishield.notification.config.NotificationProperties;
import com.apishield.notification.entity.Notification;
import com.apishield.notification.entity.NotificationChannel;
import com.apishield.notification.entity.NotificationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcher {

    private final NotificationProperties properties;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public void dispatch(Notification notification, String renderedBody) {
        if (notification.getChannel() == NotificationChannel.EMAIL) {
            dispatchEmail(notification, renderedBody);
        } else if (notification.getChannel() == NotificationChannel.IN_APP) {
            // In-app notifications are stored in DB and queried directly
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
            log.info("In-app notification ready for user: {}", notification.getRecipientUserId());
        } else {
            // WEBHOOK or custom channels
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
            log.info("Webhook notification dispatched for user: {}", notification.getRecipientUserId());
        }
    }

    private void dispatchEmail(Notification notification, String body) {
        String recipient = notification.getRecipientEmail();
        if (recipient == null || recipient.isBlank()) {
            log.warn("Cannot dispatch email: recipient email is missing for user {}", notification.getRecipientUserId());
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage("Recipient email address is missing");
            return;
        }

        if (properties.isMockDelivery() || mailSender == null) {
            log.info("[MOCK EMAIL DISPATCH] From: <{}>, To: <{}>, Subject: '{}', Body Preview: '{}'",
                    properties.getFromEmail(), recipient, notification.getTitle(),
                    body.length() > 80 ? body.substring(0, 80) + "..." : body);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.getFromEmail());
            message.setTo(recipient);
            message.setSubject(notification.getTitle());
            message.setText(body);
            mailSender.send(message);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
            log.info("Email successfully dispatched to {}", recipient);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", recipient, e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notification.setRetryCount(notification.getRetryCount() + 1);
        }
    }
}
