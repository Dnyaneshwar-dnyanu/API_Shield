package com.apishield.subscription.controller;

import com.apishield.common.constant.HeaderConstants;
import com.apishield.subscription.client.RegistryServiceClient;
import com.apishield.subscription.config.TestSubscriptionConfig;
import com.apishield.subscription.dto.CreateSubscriptionRequest;
import com.apishield.subscription.dto.client.PlanResponse;
import com.apishield.subscription.model.SubscriptionStatus;
import com.apishield.subscription.repository.SubscriptionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSubscriptionConfig.class)
class SubscriptionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private RegistryServiceClient registryClient;

    private static final String DEVELOPER_ID = "dev-user-uuid-101";
    private static final String API_ID = "api-uuid-202";
    private static final UUID FREE_PLAN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PAID_PLAN_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        subscriptionRepository.deleteAll();

        // Mock Free Plan
        Mockito.when(registryClient.getPlanById(eq(FREE_PLAN_ID))).thenReturn(
                PlanResponse.builder()
                        .id(FREE_PLAN_ID)
                        .apiId(UUID.fromString("00000000-0000-0000-0000-000000000202"))
                        .name("Free Tier")
                        .planType("FREE")
                        .priceCents(0)
                        .billingCycle("MONTHLY")
                        .rateLimitRps(10)
                        .quotaMonthly(10000)
                        .isActive(true)
                        .build()
        );

        // Mock Paid Plan
        Mockito.when(registryClient.getPlanById(eq(PAID_PLAN_ID))).thenReturn(
                PlanResponse.builder()
                        .id(PAID_PLAN_ID)
                        .apiId(UUID.fromString("00000000-0000-0000-0000-000000000202"))
                        .name("Pro Tier")
                        .planType("PAID")
                        .priceCents(4900)
                        .billingCycle("MONTHLY")
                        .rateLimitRps(100)
                        .quotaMonthly(1000000)
                        .isActive(true)
                        .build()
        );
    }

    @Test
    @DisplayName("Should subscribe to Free plan and auto-activate immediately")
    void shouldSubscribeToFreePlanAndAutoActivate() throws Exception {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .apiId("00000000-0000-0000-0000-000000000202")
                .planId(FREE_PLAN_ID.toString())
                .build();

        mockMvc.perform(post("/api/v1/subscriptions")
                        .header(HeaderConstants.X_USER_ID, DEVELOPER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.developerId").value(DEVELOPER_ID))
                .andExpect(jsonPath("$.apiId").value("00000000-0000-0000-0000-000000000202"))
                .andExpect(jsonPath("$.planId").value(FREE_PLAN_ID.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currentPeriodStart").isNotEmpty())
                .andExpect(jsonPath("$.currentPeriodEnd").isNotEmpty());

        assertThat(subscriptionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should subscribe to Paid plan in PENDING status pending payment checkout")
    void shouldSubscribeToPaidPlanInPendingStatus() throws Exception {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .apiId("00000000-0000-0000-0000-000000000202")
                .planId(PAID_PLAN_ID.toString())
                .build();

        mockMvc.perform(post("/api/v1/subscriptions")
                        .header(HeaderConstants.X_USER_ID, DEVELOPER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("Should reject duplicate active or pending subscriptions for the same API")
    void shouldRejectDuplicateSubscription() throws Exception {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .apiId("00000000-0000-0000-0000-000000000202")
                .planId(FREE_PLAN_ID.toString())
                .build();

        // First subscription succeeds
        mockMvc.perform(post("/api/v1/subscriptions")
                        .header(HeaderConstants.X_USER_ID, DEVELOPER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate subscription rejected with Conflict
        mockMvc.perform(post("/api/v1/subscriptions")
                        .header(HeaderConstants.X_USER_ID, DEVELOPER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    @DisplayName("Should reject subscription when plan belongs to a different API")
    void shouldRejectPlanMismatch() throws Exception {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .apiId("99999999-9999-9999-9999-999999999999") // Different API ID
                .planId(FREE_PLAN_ID.toString())
                .build();

        mockMvc.perform(post("/api/v1/subscriptions")
                        .header(HeaderConstants.X_USER_ID, DEVELOPER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    @DisplayName("Should list authenticated developer's subscriptions")
    void shouldListDeveloperSubscriptions() throws Exception {
        createTestSubscription(FREE_PLAN_ID);

        mockMvc.perform(get("/api/v1/subscriptions/my")
                        .header(HeaderConstants.X_USER_ID, DEVELOPER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].developerId").value(DEVELOPER_ID));
    }

    @Test
    @DisplayName("Should retrieve subscription details by ID")
    void shouldGetSubscriptionById() throws Exception {
        String subId = createTestSubscription(FREE_PLAN_ID);

        mockMvc.perform(get("/api/v1/subscriptions/" + subId)
                        .header(HeaderConstants.X_USER_ID, DEVELOPER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(subId))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Should cancel an active subscription")
    void shouldCancelSubscription() throws Exception {
        String subId = createTestSubscription(FREE_PLAN_ID);

        mockMvc.perform(put("/api/v1/subscriptions/" + subId + "/cancel")
                        .header(HeaderConstants.X_USER_ID, DEVELOPER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty());

        // Subsequent cancel attempt rejected
        mockMvc.perform(put("/api/v1/subscriptions/" + subId + "/cancel")
                        .header(HeaderConstants.X_USER_ID, DEVELOPER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Subscription Lifecycle State"));
    }

    private String createTestSubscription(UUID planId) throws Exception {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .apiId("00000000-0000-0000-0000-000000000202")
                .planId(planId.toString())
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/subscriptions")
                        .header(HeaderConstants.X_USER_ID, DEVELOPER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.get("id").asText();
    }
}
