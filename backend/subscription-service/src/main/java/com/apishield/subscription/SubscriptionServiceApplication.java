package com.apishield.subscription;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration;

/**
 * APIShield Subscription Service.
 * Central authority for developer subscriptions, state machine transitions, and plan quota validations.
 */
@SpringBootApplication(exclude = {SimpleDiscoveryClientAutoConfiguration.class})
public class SubscriptionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubscriptionServiceApplication.class, args);
    }
}
