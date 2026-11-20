package com.apishield.userservice.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    private String secret;
    private String issuer = "apishield-user-service";
    private long expirationSeconds = 3600;
    private long refreshExpirationSeconds = 604800;
    private String refreshCookieName = "refresh_token";
    private boolean refreshCookieSecure = false;
    private String refreshCookieSameSite = "Lax";
    private String refreshCookiePath = "/api/v1/auth";
}
