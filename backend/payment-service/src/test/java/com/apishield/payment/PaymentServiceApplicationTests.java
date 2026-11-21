package com.apishield.payment;

import com.apishield.payment.config.TestPaymentConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestPaymentConfig.class)
class PaymentServiceApplicationTests {

    @Test
    @DisplayName("Payment Service application context should load successfully")
    void contextLoads() {
    }
}
