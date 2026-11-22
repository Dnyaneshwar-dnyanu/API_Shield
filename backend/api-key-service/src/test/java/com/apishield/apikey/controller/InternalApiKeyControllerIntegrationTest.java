package com.apishield.apikey.controller;

import com.apishield.apikey.config.TestApiKeyConfig;
import com.apishield.apikey.dto.VerifyKeyRequest;
import com.apishield.apikey.entity.ApiKey;
import com.apishield.apikey.model.KeyStatus;
import com.apishield.apikey.repository.ApiKeyRepository;
import com.apishield.common.util.CryptoUtils;
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

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestApiKeyConfig.class)
class InternalApiKeyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    private final String rawApiKey = "ash_live_testpref_abcdef1234567890abcdef1234567890";
    private final String keyHash = CryptoUtils.sha256Hex(rawApiKey);

    @BeforeEach
    void setUp() {
        apiKeyRepository.deleteAll();
    }

    @Test
    @DisplayName("Should successfully verify valid active API key via raw key")
    void shouldVerifyValidRawKey() throws Exception {
        ApiKey key = ApiKey.builder()
                .name("Test Active Key")
                .developerId(UUID.randomUUID().toString())
                .subscriptionId(UUID.randomUUID().toString())
                .apiId(UUID.randomUUID().toString())
                .keyPrefix("ash_live_testpref")
                .keyHash(keyHash)
                .status(KeyStatus.ACTIVE)
                .rateLimitRps(50)
                .quotaMonthly(100000)
                .build();
        apiKeyRepository.save(key);

        VerifyKeyRequest request = VerifyKeyRequest.builder()
                .apiKey(rawApiKey)
                .build();

        mockMvc.perform(post("/api/v1/internal/keys/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.rateLimitRps", is(50)))
                .andExpect(jsonPath("$.quotaMonthly", is(100000)));
    }

    @Test
    @DisplayName("Should return valid=false for revoked API key")
    void shouldRejectRevokedKey() throws Exception {
        ApiKey key = ApiKey.builder()
                .name("Revoked Key")
                .developerId(UUID.randomUUID().toString())
                .subscriptionId(UUID.randomUUID().toString())
                .apiId(UUID.randomUUID().toString())
                .keyPrefix("ash_live_testpref")
                .keyHash(keyHash)
                .status(KeyStatus.REVOKED)
                .rateLimitRps(10)
                .quotaMonthly(10000)
                .build();
        apiKeyRepository.save(key);

        VerifyKeyRequest request = VerifyKeyRequest.builder()
                .keyHash(keyHash)
                .build();

        mockMvc.perform(post("/api/v1/internal/keys/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)))
                .andExpect(jsonPath("$.status", is("REVOKED")));
    }

    @Test
    @DisplayName("Should return valid=false for non-existent API key")
    void shouldRejectNonExistentKey() throws Exception {
        VerifyKeyRequest request = VerifyKeyRequest.builder()
                .keyHash("non_existent_hash_1234567890")
                .build();

        mockMvc.perform(post("/api/v1/internal/keys/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)))
                .andExpect(jsonPath("$.message", is("API key not found")));
    }
}
