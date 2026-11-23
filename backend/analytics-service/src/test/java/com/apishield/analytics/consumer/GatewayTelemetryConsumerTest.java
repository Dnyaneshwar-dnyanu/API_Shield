package com.apishield.analytics.consumer;

import com.apishield.analytics.dto.GatewayTelemetryMessage;
import com.apishield.analytics.service.AnalyticsIngestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GatewayTelemetryConsumerTest {

    @Mock
    private AnalyticsIngestionService ingestionService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private GatewayTelemetryConsumer consumer;

    @Test
    @DisplayName("Should consume and parse JSON telemetry message successfully")
    void shouldConsumeAndParseTelemetryMessage() {
        String json = "{\"correlationId\":\"corr-999\",\"apiId\":\"api-weather\",\"developerId\":\"dev-888\",\"statusCode\":200,\"responseTimeMs\":45}";

        consumer.consumeTelemetry(json);

        verify(ingestionService, times(1)).ingest(any(GatewayTelemetryMessage.class));
    }

    @Test
    @DisplayName("Should gracefully handle malformed JSON")
    void shouldGracefullyHandleMalformedJson() {
        String badJson = "{invalid-json";

        consumer.consumeTelemetry(badJson);

        verify(ingestionService, never()).ingest(any());
    }
}
