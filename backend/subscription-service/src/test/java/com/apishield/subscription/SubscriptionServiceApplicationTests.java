package com.apishield.subscription;

import com.apishield.subscription.config.TestSubscriptionConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestSubscriptionConfig.class)
class SubscriptionServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
