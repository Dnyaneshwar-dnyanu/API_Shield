package com.apishield.discoveryserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Bean;

/**
 * APIShield Netflix Eureka Service Discovery Server.
 * Acts as the centralized registry for all internal microservices and the API Gateway.
 */
@SpringBootApplication(exclude = {SimpleDiscoveryClientAutoConfiguration.class})
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }

    @Bean
    public ServerProperties serverProperties() {
        return new ServerProperties();
    }
}
