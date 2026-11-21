package com.apishield.payment.service;

import com.apishield.common.constant.KafkaTopics;
import com.apishield.common.event.EventEnvelope;
import com.apishield.common.event.PaymentCompletedEvent;
import com.apishield.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPaymentCompleted(Payment payment) {
        try {
            PaymentCompletedEvent payload = PaymentCompletedEvent.builder()
                    .paymentId(payment.getId().toString())
                    .subscriptionId(payment.getSubscriptionId())
                    .developerId(payment.getDeveloperId())
                    .amountCents(payment.getAmountCents())
                    .currency(payment.getCurrency())
                    .provider(payment.getProvider())
                    .transactionReference(payment.getPaymentId())
                    .completedAt(Instant.now())
                    .build();

            EventEnvelope<PaymentCompletedEvent> envelope = EventEnvelope.of(
                    "payment.completed",
                    "payment-service",
                    null,
                    payment.getSubscriptionId(),
                    payload
            );

            kafkaTemplate.send(KafkaTopics.PAYMENT_EVENTS, payment.getSubscriptionId(), envelope);
            log.info("Published payment.completed event for payment: {}, subscription: {}",
                    payment.getId(), payment.getSubscriptionId());
        } catch (Exception e) {
            log.warn("Failed to publish payment.completed event for payment {}: {}", payment.getId(), e.getMessage());
        }
    }

    public void publishPaymentFailed(Payment payment, String reason) {
        try {
            Map<String, Object> payload = Map.of(
                    "paymentId", payment.getId().toString(),
                    "subscriptionId", payment.getSubscriptionId(),
                    "developerId", payment.getDeveloperId(),
                    "reason", reason != null ? reason : "Payment failed",
                    "failedAt", Instant.now().toString()
            );

            EventEnvelope<Map<String, Object>> envelope = EventEnvelope.of(
                    "payment.failed",
                    "payment-service",
                    null,
                    payment.getSubscriptionId(),
                    payload
            );

            kafkaTemplate.send(KafkaTopics.PAYMENT_EVENTS, payment.getSubscriptionId(), envelope);
            log.info("Published payment.failed event for payment: {}, subscription: {}",
                    payment.getId(), payment.getSubscriptionId());
        } catch (Exception e) {
            log.warn("Failed to publish payment.failed event for payment {}: {}", payment.getId(), e.getMessage());
        }
    }
}
