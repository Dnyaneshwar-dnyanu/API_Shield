package com.apishield.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRotationResult {

    private String newAccessToken;
    private String newRawRefreshToken;
    private long expiresIn;
    private UserResponse user;
}
