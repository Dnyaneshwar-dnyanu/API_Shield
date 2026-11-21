package com.apishield.payment.config;

import com.apishield.payment.client.RegistryClient;
import com.apishield.payment.client.SubscriptionClient;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;

@TestConfiguration
public class TestPaymentConfig {

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return Mockito.mock(KafkaTemplate.class);
    }

    @Bean
    @Primary
    public SubscriptionClient subscriptionClient() {
        return Mockito.mock(SubscriptionClient.class);
    }

    @Bean
    @Primary
    public RegistryClient registryClient() {
        return Mockito.mock(RegistryClient.class);
    }
}

