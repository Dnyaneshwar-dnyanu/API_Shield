package com.apishield.subscription.service;

import com.apishield.subscription.dto.CreateSubscriptionRequest;
import com.apishield.subscription.dto.SubscriptionResponse;
import com.apishield.subscription.dto.SubscriptionValidationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SubscriptionService {

    SubscriptionResponse createSubscription(CreateSubscriptionRequest request, String developerId);

    Page<SubscriptionResponse> getMySubscriptions(String developerId, Pageable pageable);

    SubscriptionResponse getSubscriptionById(UUID id, String developerId);

    SubscriptionResponse cancelSubscription(UUID id, String developerId);

    SubscriptionValidationResponse validateSubscription(String developerId, String apiId);

    SubscriptionResponse getInternalSubscriptionById(UUID id);

    void activateSubscriptionFromPayment(String subscriptionId);

    void suspendSubscriptionFromPayment(String subscriptionId);
}
