package com.apishield.apikey.controller;

import com.apishield.apikey.client.SubscriptionValidationClient;
import com.apishield.apikey.config.TestApiKeyConfig;
import com.apishield.apikey.dto.CreateKeyRequest;
import com.apishield.apikey.dto.RevokeKeyRequest;
import com.apishield.apikey.dto.SubscriptionValidationDto;
import com.apishield.apikey.entity.ApiKey;
import com.apishield.apikey.model.KeyStatus;
import com.apishield.apikey.repository.ApiKeyRepository;
import com.apishield.common.constant.HeaderConstants;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestApiKeyConfig.class)
class ApiKeyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private SubscriptionValidationClient subscriptionValidationClient;

    private final String developerId = UUID.randomUUID().toString();
    private final String apiId = UUID.randomUUID().toString();
    private final String subscriptionId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        apiKeyRepository.deleteAll();
    }

    @Test
    @DisplayName("Should successfully generate a new API key and return raw plaintext once")
    void shouldGenerateApiKey() throws Exception {
        SubscriptionValidationDto subDto = SubscriptionValidationDto.builder()
                .valid(true)
                .subscriptionId(UUID.fromString(subscriptionId))
                .status("ACTIVE")
                .rateLimitRps(25)
                .quotaMonthly(50000)
                .build();

        Mockito.when(subscriptionValidationClient.validateSubscription(developerId, apiId))
                .thenReturn(Optional.of(subDto));

        CreateKeyRequest request = CreateKeyRequest.builder()
                .name("Production Gateway Key")
                .apiId(apiId)
                .subscriptionId(subscriptionId)
                .build();

        mockMvc.perform(post("/api/v1/keys")
                        .header(HeaderConstants.X_USER_ID, developerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.apiKey", startsWith("ash_live_")))
                .andExpect(jsonPath("$.keyPrefix", startsWith("ash_live_")))
                .andExpect(jsonPath("$.rateLimitRps", is(25)))
                .andExpect(jsonPath("$.quotaMonthly", is(50000)))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        assertThat(apiKeyRepository.findAll()).hasSize(1);
        ApiKey saved = apiKeyRepository.findAll().get(0);
        assertThat(saved.getStatus()).isEqualTo(KeyStatus.ACTIVE);
        assertThat(saved.getKeyHash()).isNotBlank();
        // Crucial security test: raw key must NEVER be stored in the DB entity
        assertThat(saved.getKeyHash()).isNotEqualTo(saved.getKeyPrefix());
    }

    @Test
    @DisplayName("Should return 409 Conflict when developer subscription is not ACTIVE")
    void shouldRejectWhenSubscriptionNotActive() throws Exception {
        SubscriptionValidationDto subDto = SubscriptionValidationDto.builder()
                .valid(false)
                .subscriptionId(UUID.fromString(subscriptionId))
                .status("PENDING")
                .build();

        Mockito.when(subscriptionValidationClient.validateSubscription(developerId, apiId))
                .thenReturn(Optional.of(subDto));

        CreateKeyRequest request = CreateKeyRequest.builder()
                .name("Test Key")
                .apiId(apiId)
                .subscriptionId(subscriptionId)
                .build();

        mockMvc.perform(post("/api/v1/keys")
                        .header(HeaderConstants.X_USER_ID, developerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("Conflict")));
    }

    @Test
    @DisplayName("Should return list of developer keys with safe metadata")
    void shouldListDeveloperKeys() throws Exception {
        ApiKey key = ApiKey.builder()
                .name("Client Mobile App")
                .developerId(developerId)
                .subscriptionId(subscriptionId)
                .apiId(apiId)
                .keyPrefix("ash_live_abcdef12")
                .keyHash("1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef")
                .status(KeyStatus.ACTIVE)
                .rateLimitRps(10)
                .quotaMonthly(10000)
                .build();
        apiKeyRepository.save(key);

        mockMvc.perform(get("/api/v1/keys")
                        .header(HeaderConstants.X_USER_ID, developerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Client Mobile App")))
                .andExpect(jsonPath("$[0].keyPrefix", is("ash_live_abcdef12")))
                .andExpect(jsonPath("$[0].apiKey").doesNotExist()) // Raw key must never be exposed!
                .andExpect(jsonPath("$[0].keyHash").doesNotExist()); // Hash must never be exposed!
    }

    @Test
    @DisplayName("Should revoke active API key")
    void shouldRevokeKey() throws Exception {
        ApiKey key = ApiKey.builder()
                .name("Old Key")
                .developerId(developerId)
                .subscriptionId(subscriptionId)
                .apiId(apiId)
                .keyPrefix("ash_live_12345678")
                .keyHash("hash_to_revoke_1234567890abcdef")
                .status(KeyStatus.ACTIVE)
                .build();
        key = apiKeyRepository.save(key);

        RevokeKeyRequest revokeRequest = RevokeKeyRequest.builder()
                .reason("Compromised credentials")
                .build();

        mockMvc.perform(post("/api/v1/keys/" + key.getId() + "/revoke")
                        .header(HeaderConstants.X_USER_ID, developerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revokeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REVOKED")));

        ApiKey updated = apiKeyRepository.findById(key.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(KeyStatus.REVOKED);
    }

    @Test
    @DisplayName("Should roll API key, revoking old and returning new plaintext key")
    void shouldRollKey() throws Exception {
        ApiKey oldKey = ApiKey.builder()
                .name("Rolling Key")
                .developerId(developerId)
                .subscriptionId(subscriptionId)
                .apiId(apiId)
                .keyPrefix("ash_live_oldprefix")
                .keyHash("hash_old_key_1234567890abcdef")
                .status(KeyStatus.ACTIVE)
                .rateLimitRps(20)
                .quotaMonthly(20000)
                .build();
        oldKey = apiKeyRepository.save(oldKey);

        mockMvc.perform(post("/api/v1/keys/" + oldKey.getId() + "/roll")
                        .header(HeaderConstants.X_USER_ID, developerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.apiKey", startsWith("ash_live_")))
                .andExpect(jsonPath("$.keyPrefix", not(is("ash_live_oldprefix"))))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        // Old key must be REVOKED
        ApiKey oldUpdated = apiKeyRepository.findById(oldKey.getId()).orElseThrow();
        assertThat(oldUpdated.getStatus()).isEqualTo(KeyStatus.REVOKED);

        // Exactly 2 keys exist in DB (1 REVOKED, 1 ACTIVE)
        assertThat(apiKeyRepository.findAll()).hasSize(2);
    }
}
