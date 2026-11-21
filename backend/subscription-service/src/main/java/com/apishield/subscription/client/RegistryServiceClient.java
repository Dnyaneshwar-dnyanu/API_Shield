package com.apishield.subscription.client;

import com.apishield.subscription.dto.client.PlanResponse;
import com.apishield.subscription.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegistryServiceClient {

    private final RestClient registryRestClient;

    /**
     * Queries api-registry-service to resolve and validate plan details.
     *
     * @param planId Plan identifier
     * @return Resolved PlanResponse
     */
    public PlanResponse getPlanById(UUID planId) {
        try {
            return registryRestClient.get()
                    .uri("/api/v1/internal/plans/{planId}", planId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new ResourceNotFoundException("Target plan not found with ID: " + planId);
                    })
                    .body(PlanResponse.class);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to query api-registry-service for plan {}: {}", planId, e.getMessage());
            throw new ResourceNotFoundException("Target plan could not be resolved from registry service: " + e.getMessage());
        }
    }
}
