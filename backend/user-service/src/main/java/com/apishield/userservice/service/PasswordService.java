package com.apishield.userservice.service;

import com.apishield.userservice.dto.request.ChangePasswordRequest;
import com.apishield.userservice.dto.request.ForgotPasswordRequest;
import com.apishield.userservice.dto.request.ResetPasswordRequest;

import java.util.UUID;

public interface PasswordService {

    void changePassword(UUID userId, ChangePasswordRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
