package com.apishield.registry.service.impl;

import com.apishield.registry.dto.CatalogDetailResponse;
import com.apishield.registry.dto.CatalogItemResponse;
import com.apishield.registry.dto.EndpointResponse;
import com.apishield.registry.dto.PlanResponse;
import com.apishield.registry.entity.Api;
import com.apishield.registry.entity.ApiEndpoint;
import com.apishield.registry.entity.ApiPlan;
import com.apishield.registry.exception.ResourceNotFoundException;
import com.apishield.registry.model.ApiStatus;
import com.apishield.registry.repository.ApiEndpointRepository;
import com.apishield.registry.repository.ApiPlanRepository;
import com.apishield.registry.repository.ApiRepository;
import com.apishield.registry.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final ApiRepository apiRepository;
    private final ApiEndpointRepository endpointRepository;
    private final ApiPlanRepository planRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogItemResponse> getCatalog(String category, String search, Pageable pageable) {
        String catParam = (category != null && !category.isBlank()) ? category.trim() : null;
        String searchParam = (search != null && !search.isBlank()) ? search.trim() : null;

        return apiRepository.searchCatalog(ApiStatus.PUBLISHED, catParam, searchParam, pageable)
                .map(this::mapToCatalogItem);
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogDetailResponse getCatalogItemBySlug(String slug) {
        Api api = apiRepository.findBySlug(slug.toLowerCase().trim())
                .filter(a -> a.getStatus() == ApiStatus.PUBLISHED && a.isPublic())
                .orElseThrow(() -> new ResourceNotFoundException("API not found in catalog for slug: " + slug));

        List<EndpointResponse> endpoints = endpointRepository.findByApiIdAndIsActiveTrue(api.getId())
                .stream().map(this::mapToEndpointResponse).toList();
        List<PlanResponse> plans = planRepository.findByApiIdAndIsActiveTrue(api.getId())
                .stream().map(this::mapToPlanResponse).toList();

        return CatalogDetailResponse.builder()
                .id(api.getId())
                .name(api.getName())
                .slug(api.getSlug())
                .description(api.getDescription())
                .category(api.getCategory())
                .versionLabel(api.getVersionLabel())
                .endpoints(endpoints)
                .plans(plans)
                .build();
    }

    private CatalogItemResponse mapToCatalogItem(Api api) {
        List<ApiEndpoint> endpoints = endpointRepository.findByApiIdAndIsActiveTrue(api.getId());
        List<ApiPlan> plans = planRepository.findByApiIdAndIsActiveTrue(api.getId());

        return CatalogItemResponse.builder()
                .id(api.getId())
                .name(api.getName())
                .slug(api.getSlug())
                .description(api.getDescription())
                .category(api.getCategory())
                .versionLabel(api.getVersionLabel())
                .endpointCount(endpoints.size())
                .planCount(plans.size())
                .build();
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
