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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestRegistryConfig.class)
class CatalogControllerIntegrationTest {

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

    private static final String OWNER_ID = "owner-catalog-test";

    @BeforeEach
    void setUp() {
        endpointRepository.deleteAll();
        planRepository.deleteAll();
        apiRepository.deleteAll();
    }

    @Test
    @DisplayName("Should only return PUBLISHED and PUBLIC APIs in the marketplace catalog")
    void shouldReturnOnlyPublishedApisInCatalog() throws Exception {
        // Create 1 draft API (should NOT appear)
        createApi("Draft Weather", "draft-weather", "Weather", false);

        // Create 1 published API (SHOULD appear)
        createApi("Public Stocks API", "stocks-api", "Finance", true);

        mockMvc.perform(get("/api/v1/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].slug").value("stocks-api"))
                .andExpect(jsonPath("$.content[0].category").value("Finance"));
    }

    @Test
    @DisplayName("Should filter catalog by category and search keyword")
    void shouldFilterCatalogByCategoryAndSearch() throws Exception {
        createApi("Public AI Translator", "ai-translator", "AI", true);
        createApi("Public AI Voice Synthesizer", "ai-voice", "AI", true);
        createApi("Public Crypto Tracker", "crypto-tracker", "Finance", true);

        // Filter by category AI
        mockMvc.perform(get("/api/v1/catalog")
                        .param("category", "AI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));

        // Search for "Voice"
        mockMvc.perform(get("/api/v1/catalog")
                        .param("search", "Voice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].slug").value("ai-voice"));
    }

    @Test
    @DisplayName("Should retrieve published catalog item details by slug with plans and endpoints")
    void shouldGetCatalogItemBySlug() throws Exception {
        String apiId = createApi("Email Verification", "email-verify-api", "Tools", true);

        // Query catalog by slug
        mockMvc.perform(get("/api/v1/catalog/email-verify-api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Email Verification"))
                .andExpect(jsonPath("$.slug").value("email-verify-api"))
                .andExpect(jsonPath("$.endpoints", hasSize(1)))
                .andExpect(jsonPath("$.endpoints[0].pathPattern").value("/v1/default"))
                .andExpect(jsonPath("$.plans", hasSize(1)))
                .andExpect(jsonPath("$.plans[0].name").value("Default Free Plan"));
    }

    @Test
    @DisplayName("Should return 404 when querying catalog for draft or non-existent API")
    void shouldReturn404ForDraftOrNonExistentSlug() throws Exception {
        // Create draft API (not published)
        createApi("Unpublished Draft", "unpublished-slug", "Security", false);

        mockMvc.perform(get("/api/v1/catalog/unpublished-slug"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));

        mockMvc.perform(get("/api/v1/catalog/completely-unknown-slug"))
                .andExpect(status().isNotFound());
    }

    private String createApi(String name, String slug, String category, boolean publish) throws Exception {
        CreateApiRequest request = CreateApiRequest.builder()
                .name(name)
                .slug(slug)
                .description("Marketplace test API")
                .category(category)
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
        String apiId = root.get("id").asText();

        if (publish) {
            // Add required endpoint and plan before publishing
            CreateEndpointRequest ep = CreateEndpointRequest.builder()
                    .pathPattern("/v1/default")
                    .httpMethod(HttpMethodType.GET)
                    .isActive(true)
                    .build();
            mockMvc.perform(post("/api/v1/apis/" + apiId + "/endpoints")
                            .header(HeaderConstants.X_USER_ID, OWNER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ep)))
                    .andExpect(status().isCreated());

            CreatePlanRequest pl = CreatePlanRequest.builder()
                    .name("Default Free Plan")
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

            mockMvc.perform(put("/api/v1/apis/" + apiId + "/publish")
                            .header(HeaderConstants.X_USER_ID, OWNER_ID))
                    .andExpect(status().isOk());
        }

        return apiId;
    }
}
