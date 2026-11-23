package com.apishield.analytics;

import com.apishield.analytics.config.TestAnalyticsConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestAnalyticsConfig.class)
class AnalyticsServiceApplicationTests {

    @Test
    @DisplayName("Analytics Service Spring Context should bootstrap successfully")
    void contextLoads() {
        assertThat(true).isTrue();
    }
}
