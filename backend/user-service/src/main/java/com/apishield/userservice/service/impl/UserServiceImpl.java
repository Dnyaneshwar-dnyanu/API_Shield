package com.apishield.userservice.service.impl;

import com.apishield.userservice.dto.request.UpdateProfileRequest;
import com.apishield.userservice.dto.response.UserResponse;
import com.apishield.userservice.entity.User;
import com.apishield.userservice.exception.ResourceNotFoundException;
import com.apishield.userservice.repository.UserRepository;
import com.apishield.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        log.debug("Fetching profile for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        return UserResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        log.info("Updating profile for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Normalize inputs
        String trimmedFirstName = request.getFirstName().trim();
        String trimmedLastName = request.getLastName().trim();

        user.setFirstName(trimmedFirstName);
        user.setLastName(trimmedLastName);

        User savedUser = userRepository.save(user);
        log.info("Successfully updated profile for user ID: {}", userId);

        return UserResponse.fromEntity(savedUser);
    }
}
