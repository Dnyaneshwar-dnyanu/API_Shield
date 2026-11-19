package com.apishield.userservice.service;

import com.apishield.userservice.dto.request.RegisterRequest;
import com.apishield.userservice.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);
}
