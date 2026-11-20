package com.apishield.userservice.service;

import com.apishield.userservice.dto.response.RefreshTokenResult;
import com.apishield.userservice.dto.response.TokenRotationResult;
import com.apishield.userservice.entity.User;
import org.springframework.http.ResponseCookie;

import java.util.UUID;

public interface RefreshTokenService {

    RefreshTokenResult createRefreshToken(User user);

    TokenRotationResult rotateRefreshToken(String rawRefreshToken);

    void revokeRefreshToken(String rawRefreshToken, String reason);

    void revokeAllUserSessions(UUID userId, String reason);

    ResponseCookie createRefreshTokenCookie(String rawRefreshToken);

    ResponseCookie createCleanRefreshTokenCookie();
}
