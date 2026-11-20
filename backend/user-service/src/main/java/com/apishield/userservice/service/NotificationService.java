package com.apishield.userservice.service;

public interface NotificationService {

    void sendPasswordResetEmail(String email, String rawResetToken);

    String getLatestDevResetToken(String email);

    void sendEmailVerification(String email, String rawVerificationToken);

    String getLatestDevVerificationToken(String email);
}
