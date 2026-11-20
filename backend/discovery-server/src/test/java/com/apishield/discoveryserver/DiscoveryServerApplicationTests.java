package com.apishield.discoveryserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DiscoveryServerApplicationTests {

    @Test
    @DisplayName("Should successfully bootstrap Eureka Discovery Server context")
    void contextLoads() {
        // Confirms Eureka discovery server context starts without error
    }
}
