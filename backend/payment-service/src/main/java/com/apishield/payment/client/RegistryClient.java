package com.apishield.payment.client;

import com.apishield.payment.dto.client.PlanClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Slf4j
@Component
public class RegistryClient {

    private final RestClient restClient;

    public RegistryClient(
            @Value("${services.api-registry-service.url:http://127.0.0.1:8082}") String registryServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(registryServiceUrl)
                .build();
    }

    public Optional<PlanClientResponse> getPlanById(String planId) {
        try {
            PlanClientResponse response = restClient.get()
                    .uri("/api/v1/internal/plans/{id}", planId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        log.warn("Plan not found in registry: {}", res.getStatusCode());
                    })
                    .body(PlanClientResponse.class);
            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.error("Failed to query api-registry-service for planId {}: {}", planId, e.getMessage());
            return Optional.empty();
        }
    }
}
