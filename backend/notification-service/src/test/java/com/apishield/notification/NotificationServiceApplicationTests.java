package com.apishield.notification;

import com.apishield.notification.config.TestNotificationConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestNotificationConfig.class)
class NotificationServiceApplicationTests {

    @Test
    @DisplayName("Notification Service Spring Context should bootstrap successfully")
    void contextLoads() {
        assertThat(true).isTrue();
    }
}
