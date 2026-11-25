package com.apishield.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "apishield.gateway")
public class GatewayProperties {

    private String internalSecret = "apishield-internal-secret-token-key-32bytes-min!";
    private Services services = new Services();

    @Data
    public static class Services {
        private String registryServiceUrl = "http://localhost:8082";
        private String apiKeyServiceUrl = "http://localhost:8084";
    }
}
