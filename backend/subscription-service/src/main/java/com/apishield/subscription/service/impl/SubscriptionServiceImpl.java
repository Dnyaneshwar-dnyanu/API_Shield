package com.apishield.subscription.service.impl;

import com.apishield.subscription.client.RegistryServiceClient;
import com.apishield.subscription.dto.CreateSubscriptionRequest;
import com.apishield.subscription.dto.SubscriptionResponse;
import com.apishield.subscription.dto.SubscriptionValidationResponse;
import com.apishield.subscription.dto.client.PlanResponse;
import com.apishield.subscription.entity.Subscription;
import com.apishield.subscription.exception.ConflictException;
import com.apishield.subscription.exception.ResourceNotFoundException;
import com.apishield.subscription.exception.SubscriptionLifecycleException;
import com.apishield.subscription.model.SubscriptionStatus;
import com.apishield.subscription.repository.SubscriptionRepository;
import com.apishield.subscription.service.SubscriptionEventPublisher;
import com.apishield.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final RegistryServiceClient registryClient;
    private final SubscriptionEventPublisher eventPublisher;

    @Override
    @Transactional
    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request, String developerId) {
        UUID planUuid;
        try {
            planUuid = UUID.fromString(request.getPlanId().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid plan UUID format: " + request.getPlanId());
        }

        // 1. Verify plan existence and compatibility via api-registry-service
        PlanResponse plan = registryClient.getPlanById(planUuid);
        if (!plan.getApiId().toString().equalsIgnoreCase(request.getApiId().trim())) {
            throw new IllegalArgumentException("Plan " + planUuid + " does not belong to API " + request.getApiId());
        }

        if (!plan.isActive()) {
            throw new SubscriptionLifecycleException("Requested plan is currently inactive and cannot accept new subscriptions");
        }

        // 2. Prevent duplicate active or pending subscriptions
        boolean hasExisting = subscriptionRepository.existsByDeveloperIdAndApiIdAndStatusIn(
                developerId,
                request.getApiId().trim(),
                List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PENDING)
        );

        if (hasExisting) {
            throw new ConflictException("Developer already has an active or pending subscription for API: " + request.getApiId());
        }

        // 3. Compute billing period
        Instant now = Instant.now();
        int days = "YEARLY".equalsIgnoreCase(plan.getBillingCycle()) ? 365 : 30;
        Instant periodEnd = now.plus(days, ChronoUnit.DAYS);

        // 4. Free plans auto-activate; Paid plans start in PENDING status
        boolean isFree = plan.getPriceCents() == 0;
        SubscriptionStatus initialStatus = isFree ? SubscriptionStatus.ACTIVE : SubscriptionStatus.PENDING;

        Subscription subscription = Subscription.builder()
                .developerId(developerId)
                .apiId(request.getApiId().trim())
                .planId(planUuid.toString())
                .status(initialStatus)
                .currentPeriodStart(now)
                .currentPeriodEnd(periodEnd)
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Created subscription: {} for developer: {} on API: {} (Status: {})",
                saved.getId(), developerId, saved.getApiId(), saved.getStatus());

        if (isFree) {
            eventPublisher.publishSubscriptionActivated(saved, plan.getRateLimitRps(), plan.getQuotaMonthly());
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SubscriptionResponse> getMySubscriptions(String developerId, Pageable pageable) {
        return subscriptionRepository.findByDeveloperId(developerId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscriptionById(UUID id, String developerId) {
        Subscription sub = findAndVerifyOwner(id, developerId);
        return mapToResponse(sub);
    }

    @Override
    @Transactional
    public SubscriptionResponse cancelSubscription(UUID id, String developerId) {
        Subscription sub = findAndVerifyOwner(id, developerId);

        if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new SubscriptionLifecycleException("Subscription is already cancelled");
        }

        sub.setStatus(SubscriptionStatus.CANCELLED);
        sub.setCancelledAt(Instant.now());
        Subscription updated = subscriptionRepository.save(sub);
        log.info("Cancelled subscription: {} by developer: {}", updated.getId(), developerId);

        eventPublisher.publishSubscriptionCancelled(updated);

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public SubscriptionValidationResponse validateSubscription(String developerId, String apiId) {
        var opt = subscriptionRepository.findFirstByDeveloperIdAndApiIdAndStatus(
                developerId, apiId, SubscriptionStatus.ACTIVE);

        if (opt.isEmpty()) {
            return SubscriptionValidationResponse.builder()
                    .developerId(developerId)
                    .apiId(apiId)
                    .active(false)
                    .build();
        }

        Subscription sub = opt.get();

        // Check if expired
        if (sub.getCurrentPeriodEnd().isBefore(Instant.now())) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);
            log.info("Subscription {} has expired", sub.getId());
            return SubscriptionValidationResponse.builder()
                    .subscriptionId(sub.getId())
                    .developerId(developerId)
                    .apiId(apiId)
                    .planId(sub.getPlanId())
                    .status(SubscriptionStatus.EXPIRED)
                    .active(false)
                    .build();
        }

        // Fetch up-to-date plan quotas
        int rateLimitRps = 10;
        int quotaMonthly = 10000;
        try {
            PlanResponse plan = registryClient.getPlanById(UUID.fromString(sub.getPlanId()));
            rateLimitRps = plan.getRateLimitRps();
            quotaMonthly = plan.getQuotaMonthly();
        } catch (Exception e) {
            log.warn("Could not query live plan {} quotas, using defaults: {}", sub.getPlanId(), e.getMessage());
        }

        return SubscriptionValidationResponse.builder()
                .subscriptionId(sub.getId())
                .developerId(developerId)
                .apiId(apiId)
                .planId(sub.getPlanId())
                .status(SubscriptionStatus.ACTIVE)
                .active(true)
                .rateLimitRps(rateLimitRps)
                .quotaMonthly(quotaMonthly)
                .currentPeriodEnd(sub.getCurrentPeriodEnd())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getInternalSubscriptionById(UUID id) {
        Subscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with ID: " + id));
        return mapToResponse(sub);
    }

    @Override
    @Transactional
    public void activateSubscriptionFromPayment(String subscriptionId) {
        UUID subUuid;
        try {
            subUuid = UUID.fromString(subscriptionId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid subscription UUID from payment event: {}", subscriptionId);
            return;
        }

        var opt = subscriptionRepository.findById(subUuid);
        if (opt.isEmpty()) {
            log.warn("Subscription not found for payment activation: {}", subscriptionId);
            return;
        }

        Subscription sub = opt.get();
        if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
            log.warn("Cannot activate cancelled subscription: {}", subscriptionId);
            return;
        }

        sub.setStatus(SubscriptionStatus.ACTIVE);
        Instant now = Instant.now();
        sub.setCurrentPeriodStart(now);
        sub.setCurrentPeriodEnd(now.plus(30, ChronoUnit.DAYS));
        Subscription updated = subscriptionRepository.save(sub);
        log.info("Activated subscription: {} from payment event", updated.getId());

        int rps = 10;
        int quota = 10000;
        try {
            PlanResponse plan = registryClient.getPlanById(UUID.fromString(sub.getPlanId()));
            rps = plan.getRateLimitRps();
            quota = plan.getQuotaMonthly();
        } catch (Exception e) {
            log.warn("Could not query plan for activated subscription: {}", e.getMessage());
        }

        eventPublisher.publishSubscriptionActivated(updated, rps, quota);
    }

    @Override
    @Transactional
    public void suspendSubscriptionFromPayment(String subscriptionId) {
        UUID subUuid;
        try {
            subUuid = UUID.fromString(subscriptionId);
        } catch (IllegalArgumentException e) {
            return;
        }

        var opt = subscriptionRepository.findById(subUuid);
        if (opt.isEmpty()) {
            return;
        }

        Subscription sub = opt.get();
        if (sub.getStatus() == SubscriptionStatus.ACTIVE || sub.getStatus() == SubscriptionStatus.PENDING) {
            sub.setStatus(SubscriptionStatus.SUSPENDED);
            subscriptionRepository.save(sub);
            log.info("Suspended subscription: {} due to payment failure", sub.getId());
        }
    }

    private Subscription findAndVerifyOwner(UUID id, String developerId) {
        Subscription sub = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with ID: " + id));

        if (!sub.getDeveloperId().equalsIgnoreCase(developerId)) {
            throw new ResourceNotFoundException("Subscription not found with ID: " + id);
        }

        return sub;
    }

    private SubscriptionResponse mapToResponse(Subscription s) {
        return SubscriptionResponse.builder()
                .id(s.getId())
                .developerId(s.getDeveloperId())
                .apiId(s.getApiId())
                .planId(s.getPlanId())
                .status(s.getStatus())
                .currentPeriodStart(s.getCurrentPeriodStart())
                .currentPeriodEnd(s.getCurrentPeriodEnd())
                .cancelledAt(s.getCancelledAt())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
