package com.apishield.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "apishield.gateway")
public class ApiShieldGatewayProperties {

    private String internalSecret = "apishield-internal-secret-token-key-32bytes-min!";
    private Services services = new Services();

    @Data
    public static class Services {
        private String registryServiceUrl = "http://127.0.0.1:8082";
        private String apiKeyServiceUrl = "http://127.0.0.1:8084";
    }
}
