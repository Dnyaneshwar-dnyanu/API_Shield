package com.apishield.analytics.controller;

import com.apishield.analytics.config.TestAnalyticsConfig;
import com.apishield.analytics.dto.GatewayTelemetryMessage;
import com.apishield.analytics.entity.RequestLog;
import com.apishield.analytics.entity.UsageStatistic;
import com.apishield.analytics.repository.RequestLogRepository;
import com.apishield.analytics.repository.UsageStatisticRepository;
import com.apishield.common.constant.HeaderConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestAnalyticsConfig.class)
class AnalyticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RequestLogRepository requestLogRepository;

    @Autowired
    private UsageStatisticRepository usageStatisticRepository;

    private static final String INTERNAL_TOKEN = "test-internal-secret-token-key-32bytes-min!";

    @BeforeEach
    void cleanUp() {
        requestLogRepository.deleteAll();
        usageStatisticRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/v1/analytics/owner/overview should return aggregate metrics")
    void shouldReturnOwnerOverview() throws Exception {
        usageStatisticRepository.save(UsageStatistic.builder()
                .apiId("api-crypto-1")
                .developerId("dev-101")
                .periodDate(LocalDate.now())
                .periodHour(10)
                .totalRequests(100)
                .successfulRequests(95)
                .clientErrors(4)
                .serverErrors(1)
                .avgLatencyMs(50)
                .build());

        requestLogRepository.save(RequestLog.builder()
                .requestId("req-ov-1")
                .apiId("api-crypto-1")
                .endpointPath("/v1/crypto/price")
                .httpMethod("GET")
                .statusCode(200)
                .latencyMs(50)
                .timestamp(Instant.now())
                .build());

        mockMvc.perform(get("/api/v1/analytics/owner/overview")
                        .param("apiId", "api-crypto-1")
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests", is(100)))
                .andExpect(jsonPath("$.successfulRequests", is(95)))
                .andExpect(jsonPath("$.errorCount", is(5)))
                .andExpect(jsonPath("$.errorRate", is(5.0)))
                .andExpect(jsonPath("$.averageLatencyMs", is(50.0)));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/owner/apis/{apiId}/timeseries should return chart points")
    void shouldReturnTimeseries() throws Exception {
        usageStatisticRepository.save(UsageStatistic.builder()
                .apiId("api-crypto-1")
                .developerId("dev-101")
                .periodDate(LocalDate.now())
                .periodHour(14)
                .totalRequests(40)
                .successfulRequests(40)
                .clientErrors(0)
                .serverErrors(0)
                .avgLatencyMs(30)
                .build());

        mockMvc.perform(get("/api/v1/analytics/owner/apis/api-crypto-1/timeseries")
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].totalRequests", is(40)))
                .andExpect(jsonPath("$[0].successfulRequests", is(40)));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/developer/quota should return current month usage")
    void shouldReturnDeveloperQuota() throws Exception {
        usageStatisticRepository.save(UsageStatistic.builder()
                .apiId("api-stock-1")
                .developerId("dev-alice")
                .periodDate(LocalDate.now())
                .periodHour(9)
                .totalRequests(520)
                .successfulRequests(520)
                .build());

        mockMvc.perform(get("/api/v1/analytics/developer/quota")
                        .header(HeaderConstants.X_USER_ID, "dev-alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.developerId", is("dev-alice")))
                .andExpect(jsonPath("$.currentMonthUsage", is(520)));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/developer/logs should return paginated request history")
    void shouldReturnDeveloperLogs() throws Exception {
        requestLogRepository.save(RequestLog.builder()
                .requestId("req-log-1")
                .apiId("api-stock-1")
                .apiSlug("stock-api")
                .developerId("dev-bob")
                .endpointPath("/v1/stocks/aapl")
                .httpMethod("GET")
                .statusCode(200)
                .latencyMs(32)
                .timestamp(Instant.now())
                .build());

        mockMvc.perform(get("/api/v1/analytics/developer/logs")
                        .header(HeaderConstants.X_USER_ID, "dev-bob")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].requestId", is("req-log-1")))
                .andExpect(jsonPath("$.content[0].apiSlug", is("stock-api")));
    }

    @Test
    @DisplayName("POST /api/v1/analytics/telemetry/ingest should accept telemetry with valid internal token")
    void shouldAcceptTelemetryWithValidToken() throws Exception {
        GatewayTelemetryMessage message = GatewayTelemetryMessage.builder()
                .correlationId("corr-telemetry-1")
                .apiId("api-weather-1")
                .apiSlug("weather-api")
                .developerId("dev-charlie")
                .endpointPath("/v1/weather/current")
                .httpMethod("GET")
                .statusCode(200)
                .responseTimeMs(45)
                .timestamp(Instant.now())
                .build();

        mockMvc.perform(post("/api/v1/analytics/telemetry/ingest")
                        .header(HeaderConstants.X_INTERNAL_TOKEN, INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(message)))
                .andExpect(status().isAccepted());

        assertThat(requestLogRepository.count()).isEqualTo(1L);
        assertThat(usageStatisticRepository.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("POST /api/v1/analytics/telemetry/ingest should reject with 401 when token is invalid")
    void shouldRejectTelemetryWithInvalidToken() throws Exception {
        GatewayTelemetryMessage message = GatewayTelemetryMessage.builder()
                .correlationId("corr-telemetry-bad")
                .apiId("api-weather-1")
                .build();

        mockMvc.perform(post("/api/v1/analytics/telemetry/ingest")
                        .header(HeaderConstants.X_INTERNAL_TOKEN, "bad-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(message)))
                .andExpect(status().isUnauthorized());

        assertThat(requestLogRepository.count()).isEqualTo(0L);
    }
}
