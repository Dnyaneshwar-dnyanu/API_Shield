package com.apishield.userservice.service;

import com.apishield.userservice.dto.request.DeactivateAccountRequest;
import com.apishield.userservice.dto.request.ResendVerificationRequest;
import com.apishield.userservice.dto.request.VerifyEmailRequest;

import java.util.UUID;

public interface AccountService {

    void verifyEmail(VerifyEmailRequest request);

    void resendVerificationEmail(ResendVerificationRequest request);

    void deactivateAccount(UUID userId, DeactivateAccountRequest request);

    void suspendAccount(UUID userId, String reason, UUID adminId);

    void reactivateAccount(UUID userId, UUID adminId);
}
