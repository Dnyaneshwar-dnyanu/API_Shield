package com.apishield.userservice.service;

import com.apishield.userservice.dto.request.UpdateProfileRequest;
import com.apishield.userservice.dto.response.UserResponse;

import java.util.UUID;

public interface UserService {

    UserResponse getProfile(UUID userId);

    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);
}
