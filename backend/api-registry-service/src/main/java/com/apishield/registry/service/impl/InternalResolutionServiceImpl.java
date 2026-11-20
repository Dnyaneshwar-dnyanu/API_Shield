package com.apishield.registry.service.impl;

import com.apishield.registry.dto.EndpointResponse;
import com.apishield.registry.dto.InternalApiResolutionResponse;
import com.apishield.registry.dto.PlanResponse;
import com.apishield.registry.entity.Api;
import com.apishield.registry.entity.ApiEndpoint;
import com.apishield.registry.entity.ApiPlan;
import com.apishield.registry.exception.ResourceNotFoundException;
import com.apishield.registry.repository.ApiEndpointRepository;
import com.apishield.registry.repository.ApiPlanRepository;
import com.apishield.registry.repository.ApiRepository;
import com.apishield.registry.service.InternalResolutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InternalResolutionServiceImpl implements InternalResolutionService {

    private final ApiRepository apiRepository;
    private final ApiEndpointRepository endpointRepository;
    private final ApiPlanRepository planRepository;

    @Override
    @Transactional(readOnly = true)
    public InternalApiResolutionResponse resolveApiBySlug(String slug) {
        Api api = apiRepository.findBySlug(slug.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("API route not found for slug: " + slug));

        if (api.getStatus() != com.apishield.registry.model.ApiStatus.PUBLISHED) {
            throw new ResourceNotFoundException("API is not published for slug: " + slug);
        }

        List<EndpointResponse> endpoints = endpointRepository.findByApiIdAndIsActiveTrue(api.getId())
                .stream().map(this::mapToEndpointResponse).toList();
        List<PlanResponse> plans = planRepository.findByApiIdAndIsActiveTrue(api.getId())
                .stream().map(this::mapToPlanResponse).toList();

        return InternalApiResolutionResponse.builder()
                .apiId(api.getId())
                .slug(api.getSlug())
                .targetBackendUrl(api.getTargetBackendUrl())
                .status(api.getStatus())
                .endpoints(endpoints)
                .plans(plans)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PlanResponse getPlanById(UUID planId) {
        ApiPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("API plan not found with ID: " + planId));
        return mapToPlanResponse(plan);
    }

    private EndpointResponse mapToEndpointResponse(ApiEndpoint ep) {
        return EndpointResponse.builder()
                .id(ep.getId())
                .apiId(ep.getApi().getId())
                .pathPattern(ep.getPathPattern())
                .httpMethod(ep.getHttpMethod())
                .description(ep.getDescription())
                .isActive(ep.isActive())
                .createdAt(ep.getCreatedAt())
                .updatedAt(ep.getUpdatedAt())
                .build();
    }

    private PlanResponse mapToPlanResponse(ApiPlan plan) {
        return PlanResponse.builder()
                .id(plan.getId())
                .apiId(plan.getApi().getId())
                .name(plan.getName())
                .planType(plan.getPlanType())
                .priceCents(plan.getPriceCents())
                .billingCycle(plan.getBillingCycle())
                .rateLimitRps(plan.getRateLimitRps())
                .quotaMonthly(plan.getQuotaMonthly())
                .isActive(plan.isActive())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }
}
