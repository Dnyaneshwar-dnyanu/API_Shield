package com.apishield.userservice.dto.response;

import com.apishield.userservice.entity.RefreshToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResult {

    private String rawRefreshToken;
    private RefreshToken refreshToken;
}
