package com.apishield.gateway.service;

import com.apishield.common.constant.KafkaTopics;
import com.apishield.gateway.dto.GatewayTelemetryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayTelemetryPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTelemetry(GatewayTelemetryEvent event) {
        if (event == null) {
            return;
        }

        try {
            String key = event.getKeyId() != null ? event.getKeyId().toString() : event.getCorrelationId();
            kafkaTemplate.send(KafkaTopics.GATEWAY_ANALYTICS, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to dispatch telemetry event to Kafka [{}]: {}",
                                    KafkaTopics.GATEWAY_ANALYTICS, ex.getMessage());
                        } else {
                            log.debug("Telemetry event published for slug: {}, status: {}, duration: {}ms",
                                    event.getApiSlug(), event.getStatusCode(), event.getResponseTimeMs());
                        }
                    });
        } catch (Exception e) {
            log.warn("Exception submitting telemetry event to Kafka: {}", e.getMessage());
        }
    }
}
