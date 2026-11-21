package com.apishield.subscription.controller;

import com.apishield.subscription.client.RegistryServiceClient;
import com.apishield.subscription.config.TestSubscriptionConfig;
import com.apishield.subscription.dto.client.PlanResponse;
import com.apishield.subscription.entity.Subscription;
import com.apishield.subscription.model.SubscriptionStatus;
import com.apishield.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSubscriptionConfig.class)
class InternalSubscriptionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private RegistryServiceClient registryClient;

    private static final String DEVELOPER_ID = "dev-internal-101";
    private static final String API_ID = "api-internal-202";
    private static final UUID PLAN_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() {
        subscriptionRepository.deleteAll();

        Mockito.when(registryClient.getPlanById(eq(PLAN_ID))).thenReturn(
                PlanResponse.builder()
                        .id(PLAN_ID)
                        .apiId(UUID.fromString("00000000-0000-0000-0000-000000000202"))
                        .name("Internal Tier")
                        .rateLimitRps(50)
                        .quotaMonthly(50000)
                        .isActive(true)
                        .build()
        );
    }

    @Test
    @DisplayName("Should validate active subscription and return quotas for Gateway")
    void shouldValidateActiveSubscription() throws Exception {
        Subscription sub = Subscription.builder()
                .developerId(DEVELOPER_ID)
                .apiId(API_ID)
                .planId(PLAN_ID.toString())
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(Instant.now())
                .currentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();
        subscriptionRepository.save(sub);

        mockMvc.perform(get("/api/v1/internal/subscriptions/validate")
                        .param("developerId", DEVELOPER_ID)
                        .param("apiId", API_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.rateLimitRps").value(50))
                .andExpect(jsonPath("$.quotaMonthly").value(50000));
    }

    @Test
    @DisplayName("Should return active=false when developer has no active subscription")
    void shouldReturnInactiveForMissingSubscription() throws Exception {
        mockMvc.perform(get("/api/v1/internal/subscriptions/validate")
                        .param("developerId", "unknown-dev")
                        .param("apiId", "unknown-api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("Should retrieve internal subscription details by ID")
    void shouldGetInternalSubscriptionById() throws Exception {
        Subscription sub = Subscription.builder()
                .developerId(DEVELOPER_ID)
                .apiId(API_ID)
                .planId(PLAN_ID.toString())
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(Instant.now())
                .currentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();
        Subscription saved = subscriptionRepository.save(sub);

        mockMvc.perform(get("/api/v1/internal/subscriptions/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.developerId").value(DEVELOPER_ID))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Should return 404 when internal subscription ID does not exist")
    void shouldReturn404ForUnknownSubscriptionId() throws Exception {
        mockMvc.perform(get("/api/v1/internal/subscriptions/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }
}
