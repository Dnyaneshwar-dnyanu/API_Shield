package com.apishield.registry;

import com.apishield.registry.config.TestRegistryConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestRegistryConfig.class)
class ApiRegistryServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
