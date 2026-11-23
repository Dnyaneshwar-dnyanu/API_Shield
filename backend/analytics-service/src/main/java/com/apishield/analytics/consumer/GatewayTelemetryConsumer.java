package com.apishield.analytics.consumer;

import com.apishield.analytics.dto.GatewayTelemetryMessage;
import com.apishield.analytics.service.AnalyticsIngestionService;
import com.apishield.common.constant.KafkaTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GatewayTelemetryConsumer {

    private final AnalyticsIngestionService ingestionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.GATEWAY_ANALYTICS, groupId = "apishield-analytics-group")
    public void consumeTelemetry(String messageJson) {
        try {
            GatewayTelemetryMessage event = objectMapper.readValue(messageJson, GatewayTelemetryMessage.class);
            ingestionService.ingest(event);
            log.debug("Processed gateway telemetry log for request: {}", event.getCorrelationId());
        } catch (Exception e) {
            log.error("Failed to parse or ingest gateway telemetry message [{}]: {}", messageJson, e.getMessage());
        }
    }
}
