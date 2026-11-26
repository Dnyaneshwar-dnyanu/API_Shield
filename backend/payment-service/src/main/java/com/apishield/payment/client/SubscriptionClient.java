package com.apishield.payment.client;

import com.apishield.payment.dto.client.SubscriptionClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Slf4j
@Component
public class SubscriptionClient {

    private final RestClient restClient;

    public SubscriptionClient(
            @Value("${services.subscription-service.url:http://127.0.0.1:8083}") String subscriptionServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(subscriptionServiceUrl)
                .build();
    }

    public Optional<SubscriptionClientResponse> getSubscriptionById(String subscriptionId) {
        try {
            SubscriptionClientResponse response = restClient.get()
                    .uri("/api/v1/internal/subscriptions/{id}", subscriptionId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        log.warn("Subscription not found or client error: {}", res.getStatusCode());
                    })
                    .body(SubscriptionClientResponse.class);
            return Optional.ofNullable(response);
        } catch (Exception e) {
            log.error("Failed to query subscription-service for subscriptionId {}: {}", subscriptionId, e.getMessage());
            return Optional.empty();
        }
    }
}
