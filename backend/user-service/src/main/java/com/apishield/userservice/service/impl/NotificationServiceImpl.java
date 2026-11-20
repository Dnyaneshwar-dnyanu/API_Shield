package com.apishield.userservice.service.impl;

import com.apishield.userservice.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Development implementation of NotificationService.
 * Holds raw reset and verification tokens in thread-safe maps for automated tests and Postman development verification.
 * In production, this service will dispatch asynchronous events to the APIShield Notification Service via Kafka/RabbitMQ.
 */
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final Map<String, String> devResetTokenStore = new ConcurrentHashMap<>();
    private final Map<String, String> devVerificationTokenStore = new ConcurrentHashMap<>();

    @Override
    public void sendPasswordResetEmail(String email, String rawResetToken) {
        devResetTokenStore.put(email.toLowerCase(), rawResetToken);
        log.info("Dispatched password reset notification event for email: [PROTECTED]. (Ready for Notification Service integration)");
    }

    @Override
    public String getLatestDevResetToken(String email) {
        return devResetTokenStore.get(email.toLowerCase());
    }

    @Override
    public void sendEmailVerification(String email, String rawVerificationToken) {
        devVerificationTokenStore.put(email.toLowerCase(), rawVerificationToken);
        log.info("Dispatched email verification notification event for email: [PROTECTED]. (Ready for Notification Service integration)");
    }

    @Override
    public String getLatestDevVerificationToken(String email) {
        return devVerificationTokenStore.get(email.toLowerCase());
    }
}
