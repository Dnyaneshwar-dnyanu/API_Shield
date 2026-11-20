package com.apishield.registry.controller;

import com.apishield.common.constant.HeaderConstants;
import com.apishield.registry.config.TestRegistryConfig;
import com.apishield.registry.dto.CreateApiRequest;
import com.apishield.registry.dto.CreateEndpointRequest;
import com.apishield.registry.dto.CreatePlanRequest;
import com.apishield.registry.dto.UpdateApiRequest;
import com.apishield.registry.model.BillingCycle;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestRegistryConfig.class)
class ApiControllerIntegrationTest {

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

    private static final String OWNER_ID = "owner-test-uuid-1001";

    @BeforeEach
    void setUp() {
        endpointRepository.deleteAll();
        planRepository.deleteAll();
        apiRepository.deleteAll();
    }

    @Test
    @DisplayName("Should successfully create a new API in DRAFT status")
    void shouldCreateApiSuccessfully() throws Exception {
        CreateApiRequest request = CreateApiRequest.builder()
                .name("Weather Forecasting Service")
                .slug("weather-forecast-api")
                .description("Real-time weather data and forecast models")
                .category("Weather")
                .targetBackendUrl("https://api.weatherapi.com/v1")
                .versionLabel("v1")
                .isPublic(true)
                .build();

        mockMvc.perform(post("/api/v1/apis")
                        .header(HeaderConstants.X_USER_ID, OWNER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Weather Forecasting Service"))
                .andExpect(jsonPath("$.slug").value("weather-forecast-api"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.targetBackendUrl").value("https://api.weatherapi.com/v1"))
                .andExpect(jsonPath("$.category").value("Weather"));

        assertThat(apiRepository.findBySlug("weather-forecast-api")).isPresent();
    }

    @Test
    @DisplayName("Should reject API creation with SSRF loopback address")
    void shouldRejectApiCreationWithSsrfUrl() throws Exception {
        CreateApiRequest request = CreateApiRequest.builder()
                .name("Malicious Internal API")
                .slug("malicious-internal-api")
                .description("Attempts to hit AWS metadata")
                .category("Security")
                .targetBackendUrl("http://169.254.169.254/latest/meta-data")
                .build();

        mockMvc.perform(post("/api/v1/apis")
                        .header(HeaderConstants.X_USER_ID, OWNER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("SSRF Security Violation"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Should reject duplicate slug on API creation")
    void shouldRejectDuplicateSlug() throws Exception {
        CreateApiRequest request1 = CreateApiRequest.builder()
                .name("First Product API")
                .slug("product-catalog-api")
                .category("E-Commerce")
                .targetBackendUrl("https://api.example.com/products")
                .build();

        mockMvc.perform(post("/api/v1/apis")
                        .header(HeaderConstants.X_USER_ID, OWNER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        CreateApiRequest request2 = CreateApiRequest.builder()
                .name("Duplicate Slug API")
                .slug("product-catalog-api")
                .category("E-Commerce")
                .targetBackendUrl("https://api.other.com/products")
                .build();

        mockMvc.perform(post("/api/v1/apis")
                        .header(HeaderConstants.X_USER_ID, OWNER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    @DisplayName("Should add endpoints to an existing API")
    void shouldAddEndpointsToApi() throws Exception {
        String apiId = createSampleApi("Payment Gateway", "payment-gw-api");

        CreateEndpointRequest endpointReq = CreateEndpointRequest.builder()
                .pathPattern("/v1/charges")
                .httpMethod(HttpMethodType.POST)
                .description("Process credit card charge")
                .isActive(true)
                .build();

        mockMvc.perform(post("/api/v1/apis/" + apiId + "/endpoints")
                        .header(HeaderConstants.X_USER_ID, OWNER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(endpointReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.pathPattern").value("/v1/charges"))
                .andExpect(jsonPath("$.httpMethod").value("POST"));
    }

    @Test
    @DisplayName("Should add subscription plans to an existing API")
    void shouldAddPlansToApi() throws Exception {
        String apiId = createSampleApi("Geo IP Service", "geo-ip-api");

        CreatePlanRequest planReq = CreatePlanRequest.builder()
                .name("Pro Tier")
                .planType(PlanType.PAID)
                .priceCents(4900)
                .billingCycle(BillingCycle.MONTHLY)
                .rateLimitRps(50)
                .quotaMonthly(500000)
                .isActive(true)
                .build();

        mockMvc.perform(post("/api/v1/apis/" + apiId + "/plans")
                        .header(HeaderConstants.X_USER_ID, OWNER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(planReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Pro Tier"))
                .andExpect(jsonPath("$.planType").value("PAID"))
                .andExpect(jsonPath("$.priceCents").value(4900))
                .andExpect(jsonPath("$.rateLimitRps").value(50))
                .andExpect(jsonPath("$.quotaMonthly").value(500000));
    }

    @Test
    @DisplayName("Should publish and deprecate an API")
    void shouldPublishAndDeprecateApi() throws Exception {
        String apiId = createSampleApi("Search API", "search-engine-api");

        // Add mandatory endpoint and plan before publish
        CreateEndpointRequest ep = CreateEndpointRequest.builder()
                .pathPattern("/v1/search")
                .httpMethod(HttpMethodType.GET)
                .isActive(true)
                .build();
        mockMvc.perform(post("/api/v1/apis/" + apiId + "/endpoints")
                        .header(HeaderConstants.X_USER_ID, OWNER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ep)))
                .andExpect(status().isCreated());

        CreatePlanRequest pl = CreatePlanRequest.builder()
                .name("Basic Plan")
                .planType(PlanType.FREE)
                .priceCents(0)
                .rateLimitRps(10)
                .quotaMonthly(1000)
                .isActive(true)
                .build();
        mockMvc.perform(post("/api/v1/apis/" + apiId + "/plans")
                        .header(HeaderConstants.X_USER_ID, OWNER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pl)))
                .andExpect(status().isCreated());

        // Publish
        mockMvc.perform(put("/api/v1/apis/" + apiId + "/publish")
                        .header(HeaderConstants.X_USER_ID, OWNER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // Deprecate
        mockMvc.perform(put("/api/v1/apis/" + apiId + "/deprecate")
                        .header(HeaderConstants.X_USER_ID, OWNER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEPRECATED"));
    }

    @Test
    @DisplayName("Should retrieve API detail with endpoints and plans")
    void shouldGetApiDetail() throws Exception {
        String apiId = createSampleApi("Currency Exchange", "currency-rate-api");

        mockMvc.perform(get("/api/v1/apis/" + apiId)
                        .header(HeaderConstants.X_USER_ID, OWNER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(apiId))
                .andExpect(jsonPath("$.name").value("Currency Exchange"))
                .andExpect(jsonPath("$.slug").value("currency-rate-api"));
    }

    @Test
    @DisplayName("Should update API metadata")
    void shouldUpdateApiMetadata() throws Exception {
        String apiId = createSampleApi("Old API Name", "updatable-api");

        UpdateApiRequest updateReq = UpdateApiRequest.builder()
                .name("New Better API Name")
                .description("Updated rich description")
                .category("Finance")
                .targetBackendUrl("https://api.better.com/v2")
                .isPublic(true)
                .build();

        mockMvc.perform(put("/api/v1/apis/" + apiId)
                        .header(HeaderConstants.X_USER_ID, OWNER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Better API Name"))
                .andExpect(jsonPath("$.category").value("Finance"))
                .andExpect(jsonPath("$.targetBackendUrl").value("https://api.better.com/v2"));
    }

    private String createSampleApi(String name, String slug) throws Exception {
        CreateApiRequest request = CreateApiRequest.builder()
                .name(name)
                .slug(slug)
                .description("Sample description")
                .category("General")
                .targetBackendUrl("https://api.example.com")
                .isPublic(true)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/apis")
                        .header(HeaderConstants.X_USER_ID, OWNER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.get("id").asText();
    }
}
