package com.apishield.apikey;

import com.apishield.apikey.config.TestApiKeyConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestApiKeyConfig.class)
class ApiKeyServiceApplicationTests {

    @Test
    @DisplayName("API Key Service application context should load successfully")
    void contextLoads() {
    }
}
