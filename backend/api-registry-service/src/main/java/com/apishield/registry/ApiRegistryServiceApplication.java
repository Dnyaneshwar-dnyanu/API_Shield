package com.apishield.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration;

/**
 * APIShield API Registry Service.
 * Central authority for API catalog definitions, endpoint paths, monetization plans, and SSRF prevention.
 */
@SpringBootApplication(exclude = {SimpleDiscoveryClientAutoConfiguration.class})
public class ApiRegistryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiRegistryServiceApplication.class, args);
    }
}
