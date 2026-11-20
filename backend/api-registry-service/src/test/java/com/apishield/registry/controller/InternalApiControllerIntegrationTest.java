package com.apishield.registry.controller;

import com.apishield.common.constant.HeaderConstants;
import com.apishield.registry.config.TestRegistryConfig;
import com.apishield.registry.dto.CreateApiRequest;
import com.apishield.registry.dto.CreateEndpointRequest;
import com.apishield.registry.dto.CreatePlanRequest;
import com.apishield.registry.model.HttpMethodType;
import com.apishield.registry.model.PlanType;
import com.apishield.registry.repository.ApiEndpointRepository;
import com.apishield.registry.repository.ApiPlanRepository;
import com.apishield.registry.repository.ApiRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestRegistryConfig.class)
class InternalApiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private ApiEndpointRepository endpointRepository;

    @Autowired
    private ApiPlanRepository planRepository;

    private static final String OWNER_ID = "internal-test-owner";

    @BeforeEach
    void setUp() {
        endpointRepository.deleteAll();
        planRepository.deleteAll();
        apiRepository.deleteAll();
    }

    @Test
    @DisplayName("Should resolve published API for Gateway routing")
    void shouldResolvePublishedApiForGateway() throws Exception {
        // Create & publish API
        CreateApiRequest request = CreateApiRequest.builder()
                .name("Orders Microservice")
                .slug("orders-api")
                .category("Commerce")
                .targetBackendUrl("https://orders.internal-upstream.com")
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/apis")
                        .header(HeaderConstants.X_USER_ID, OWNER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String apiId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asText();

        // Add endpoint
        CreateEndpointRequest endpointReq = CreateEndpointRequest.builder()
                .pathPattern("/orders/{orderId}")
                .httpMethod(HttpMethodType.GET)
                .description("Fetch order by ID")
                .isActive(true)
                .build();

        mockMvc.perform(post("/api/v1/apis/" + apiId + "/endpoints")
                        .header(HeaderConstants.X_USER_ID, OWNER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(endpointReq)))
                .andExpect(status().isCreated());

        // Add plan (required before publishing)
        CreatePlanRequest planReq = CreatePlanRequest.builder()
                .name("Default Plan")
                .planType(PlanType.FREE)
                .priceCents(0)
                .rateLimitRps(20)
                .quotaMonthly(5000)
                .isActive(true)
                .build();

        mockMvc.perform(post("/api/v1/apis/" + apiId + "/plans")
                        .header(HeaderConstants.X_USER_ID, OWNER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(planReq)))
                .andExpect(status().isCreated());

        // Publish API
        mockMvc.perform(put("/api/v1/apis/" + apiId + "/publish")
                        .header(HeaderConstants.X_USER_ID, OWNER_ID))
                .andExpect(status().isOk());

        // Call internal resolution endpoint
        mockMvc.perform(get("/api/v1/internal/apis/resolve/orders-api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiId").value(apiId))
                .andExpect(jsonPath("$.slug").value("orders-api"))
                .andExpect(jsonPath("$.targetBackendUrl").value("https://orders.internal-upstream.com"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.endpoints", hasSize(1)))
                .andExpect(jsonPath("$.endpoints[0].pathPattern").value("/orders/{orderId}"))
                .andExpect(jsonPath("$.endpoints[0].httpMethod").value("GET"));
    }

    @Test
    @DisplayName("Should return 404 when Gateway resolves non-published or unknown API slug")
    void shouldReturn404ForNonPublishedApiResolution() throws Exception {
        // Create draft API (not published)
        CreateApiRequest request = CreateApiRequest.builder()
                .name("Unpublished Service")
                .slug("unpublished-api")
                .category("Commerce")
                .targetBackendUrl("https://api.example.com")
                .build();

        mockMvc.perform(post("/api/v1/apis")
                        .header(HeaderConstants.X_USER_ID, OWNER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/internal/apis/resolve/unpublished-api"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @DisplayName("Should retrieve plan details by planId for Subscription Service")
    void shouldGetPlanById() throws Exception {
        // Create API
        CreateApiRequest request = CreateApiRequest.builder()
                .name("SaaS Metrics")
                .slug("metrics-api")
                .category("Analytics")
                .targetBackendUrl("https://api.example.com")
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/apis")
                        .header(HeaderConstants.X_USER_ID, OWNER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String apiId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asText();

        // Add plan
        CreatePlanRequest planReq = CreatePlanRequest.builder()
                .name("Enterprise Plan")
                .planType(PlanType.PAID)
                .priceCents(9900)
                .rateLimitRps(200)
                .quotaMonthly(10000000)
                .isActive(true)
                .build();

        MvcResult planRes = mockMvc.perform(post("/api/v1/apis/" + apiId + "/plans")
                        .header(HeaderConstants.X_USER_ID, OWNER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(planReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String planId = objectMapper.readTree(planRes.getResponse().getContentAsString()).get("id").asText();

        // Query internal plan resolution
        mockMvc.perform(get("/api/v1/internal/plans/" + planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(planId))
                .andExpect(jsonPath("$.name").value("Enterprise Plan"))
                .andExpect(jsonPath("$.rateLimitRps").value(200))
                .andExpect(jsonPath("$.quotaMonthly").value(10000000));
    }

    @Test
    @DisplayName("Should return 404 when plan ID does not exist")
    void shouldReturn404ForUnknownPlan() throws Exception {
        mockMvc.perform(get("/api/v1/internal/plans/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }
}
