package com.apishield.apikey.client;

import com.apishield.apikey.dto.SubscriptionValidationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Slf4j
@Component
public class SubscriptionValidationClient {

    private final RestClient restClient;

    public SubscriptionValidationClient(
            @Value("${services.subscription-service.url:http://127.0.0.1:8083}") String subscriptionServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(subscriptionServiceUrl)
                .build();
    }

    public Optional<SubscriptionValidationDto> validateSubscription(String developerId, String apiId) {
        try {
            SubscriptionValidationDto response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/internal/subscriptions/validate")
                            .queryParam("developerId", developerId)
                            .queryParam("apiId", apiId)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        log.warn("Subscription validation rejected by subscription-service: {}", res.getStatusCode());
                    })
                    .body(SubscriptionValidationDto.class);
            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.error("Failed to query subscription-service validate endpoint: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
