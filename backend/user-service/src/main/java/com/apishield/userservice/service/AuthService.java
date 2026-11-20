package com.apishield.userservice.service;

import com.apishield.userservice.dto.request.LoginRequest;
import com.apishield.userservice.dto.request.RegisterRequest;
import com.apishield.userservice.dto.response.AuthResult;
import com.apishield.userservice.dto.response.UserResponse;

import java.util.UUID;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResult login(LoginRequest request);

    UserResponse getCurrentUser(UUID userId);
}
