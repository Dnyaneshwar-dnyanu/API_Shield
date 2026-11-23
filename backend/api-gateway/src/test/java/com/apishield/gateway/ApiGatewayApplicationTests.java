package com.apishield.gateway;

import com.apishield.gateway.config.TestGatewayConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestGatewayConfig.class)
class ApiGatewayApplicationTests {

    @Test
    @DisplayName("API Gateway Spring Context loads successfully")
    void contextLoads() {
        assertThat(true).isTrue();
    }
}
