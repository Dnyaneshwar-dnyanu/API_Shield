package com.apishield.registry.service.impl;

import com.apishield.registry.dto.*;
import com.apishield.registry.entity.Api;
import com.apishield.registry.entity.ApiEndpoint;
import com.apishield.registry.entity.ApiPlan;
import com.apishield.registry.exception.ConflictException;
import com.apishield.registry.exception.ResourceNotFoundException;
import com.apishield.registry.model.ApiStatus;
import com.apishield.registry.repository.ApiEndpointRepository;
import com.apishield.registry.repository.ApiPlanRepository;
import com.apishield.registry.repository.ApiRepository;
import com.apishield.registry.security.SsrfProtectionValidator;
import com.apishield.registry.service.ApiRegistryService;
import com.apishield.registry.service.RegistryCacheService;
import com.apishield.registry.service.RegistryEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiRegistryServiceImpl implements ApiRegistryService {

    private final ApiRepository apiRepository;
    private final ApiEndpointRepository endpointRepository;
    private final ApiPlanRepository planRepository;
    private final SsrfProtectionValidator ssrfValidator;
    private final RegistryCacheService cacheService;
    private final RegistryEventPublisher eventPublisher;

    @Override
    @Transactional
    public ApiResponse createApi(CreateApiRequest request, String ownerId) {
        ssrfValidator.validate(request.getTargetBackendUrl());

        if (apiRepository.existsBySlug(request.getSlug().toLowerCase())) {
            throw new ConflictException("API slug '" + request.getSlug() + "' is already registered");
        }

        Api api = Api.builder()
                .ownerId(ownerId)
                .name(request.getName().trim())
                .slug(request.getSlug().toLowerCase().trim())
                .description(request.getDescription())
                .category(request.getCategory().trim())
                .targetBackendUrl(request.getTargetBackendUrl().trim())
                .status(ApiStatus.DRAFT)
                .versionLabel(request.getVersionLabel() != null ? request.getVersionLabel() : "v1")
                .isPublic(request.isPublic())
                .build();

        Api saved = apiRepository.save(api);
        log.info("Created API: {} (ID: {}) by owner: {}", saved.getSlug(), saved.getId(), ownerId);
        return mapToApiResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiDetailResponse getApiById(UUID id, String ownerId) {
        Api api = findAndVerifyOwner(id, ownerId);
        return mapToApiDetailResponse(api);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApiResponse> getApisByOwner(String ownerId, Pageable pageable) {
        return apiRepository.findByOwnerId(ownerId, pageable).map(this::mapToApiResponse);
    }

    @Override
    @Transactional
    public ApiResponse updateApi(UUID id, UpdateApiRequest request, String ownerId) {
        ssrfValidator.validate(request.getTargetBackendUrl());
        Api api = findAndVerifyOwner(id, ownerId);

        api.setName(request.getName().trim());
        api.setDescription(request.getDescription());
        api.setCategory(request.getCategory().trim());
        api.setTargetBackendUrl(request.getTargetBackendUrl().trim());

        if (request.getVersionLabel() != null && !request.getVersionLabel().isBlank()) {
            api.setVersionLabel(request.getVersionLabel().trim());
        }
        if (request.getIsPublic() != null) {
            api.setPublic(request.getIsPublic());
        }

        Api updated = apiRepository.save(api);
        log.info("Updated API: {} (ID: {})", updated.getSlug(), updated.getId());

        if (updated.getStatus() == ApiStatus.PUBLISHED) {
            cacheService.warmApiMetadata(mapToInternalResolution(updated));
        }

        return mapToApiResponse(updated);
    }

    @Override
    @Transactional
    public EndpointResponse addEndpoint(UUID apiId, CreateEndpointRequest request, String ownerId) {
        Api api = findAndVerifyOwner(apiId, ownerId);

        ApiEndpoint endpoint = ApiEndpoint.builder()
                .api(api)
                .pathPattern(request.getPathPattern().trim())
                .httpMethod(request.getHttpMethod())
                .description(request.getDescription())
                .isActive(request.isActive())
                .build();

        api.addEndpoint(endpoint);
        ApiEndpoint saved = endpointRepository.save(endpoint);
        log.info("Added endpoint {} {} to API: {}", saved.getHttpMethod(), saved.getPathPattern(), api.getSlug());

        if (api.getStatus() == ApiStatus.PUBLISHED) {
            cacheService.warmApiMetadata(mapToInternalResolution(api));
        }

        return mapToEndpointResponse(saved);
    }

    @Override
    @Transactional
    public PlanResponse addPlan(UUID apiId, CreatePlanRequest request, String ownerId) {
        Api api = findAndVerifyOwner(apiId, ownerId);

        ApiPlan plan = ApiPlan.builder()
                .api(api)
                .name(request.getName().trim())
                .planType(request.getPlanType())
                .priceCents(request.getPriceCents())
                .billingCycle(request.getBillingCycle())
                .rateLimitRps(request.getRateLimitRps())
                .quotaMonthly(request.getQuotaMonthly())
                .isActive(request.isActive())
                .build();

        api.addPlan(plan);
        ApiPlan saved = planRepository.save(plan);
        log.info("Added plan {} (type: {}) to API: {}", saved.getName(), saved.getPlanType(), api.getSlug());

        if (api.getStatus() == ApiStatus.PUBLISHED) {
            cacheService.warmApiMetadata(mapToInternalResolution(api));
        }

        return mapToPlanResponse(saved);
    }

    @Override
    @Transactional
    public ApiResponse publishApi(UUID id, String ownerId) {
        Api api = findAndVerifyOwner(id, ownerId);

        List<ApiEndpoint> endpoints = endpointRepository.findByApiIdAndIsActiveTrue(id);
        if (endpoints.isEmpty()) {
            throw new IllegalArgumentException("API must have at least one active endpoint before publishing");
        }

        List<ApiPlan> plans = planRepository.findByApiIdAndIsActiveTrue(id);
        if (plans.isEmpty()) {
            throw new IllegalArgumentException("API must have at least one active plan before publishing");
        }

        api.setStatus(ApiStatus.PUBLISHED);
        Api published = apiRepository.save(api);
        log.info("Published API: {} (ID: {})", published.getSlug(), published.getId());

        cacheService.warmApiMetadata(mapToInternalResolution(published));
        eventPublisher.publishApiPublished(published);

        return mapToApiResponse(published);
    }

    @Override
    @Transactional
    public ApiResponse deprecateApi(UUID id, String ownerId) {
        Api api = findAndVerifyOwner(id, ownerId);

        api.setStatus(ApiStatus.DEPRECATED);
        Api deprecated = apiRepository.save(api);
        log.info("Deprecated API: {} (ID: {})", deprecated.getSlug(), deprecated.getId());

        cacheService.evictApiMetadata(deprecated.getSlug());
        eventPublisher.publishApiDeprecated(deprecated);

        return mapToApiResponse(deprecated);
    }

    private Api findAndVerifyOwner(UUID id, String ownerId) {
        Api api = apiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("API not found with ID: " + id));

        if (ownerId != null && !ownerId.equals("admin") && !api.getOwnerId().equals(ownerId)) {
            throw new ResourceNotFoundException("API not found or not accessible by this owner");
        }
        return api;
    }

    private ApiResponse mapToApiResponse(Api api) {
        return ApiResponse.builder()
                .id(api.getId())
                .ownerId(api.getOwnerId())
                .name(api.getName())
                .slug(api.getSlug())
                .description(api.getDescription())
                .category(api.getCategory())
                .targetBackendUrl(api.getTargetBackendUrl())
                .status(api.getStatus())
                .versionLabel(api.getVersionLabel())
                .isPublic(api.isPublic())
                .createdAt(api.getCreatedAt())
                .updatedAt(api.getUpdatedAt())
                .build();
    }

    private ApiDetailResponse mapToApiDetailResponse(Api api) {
        List<EndpointResponse> endpoints = endpointRepository.findByApiId(api.getId())
                .stream().map(this::mapToEndpointResponse).toList();
        List<PlanResponse> plans = planRepository.findByApiId(api.getId())
                .stream().map(this::mapToPlanResponse).toList();

        return ApiDetailResponse.builder()
                .id(api.getId())
                .ownerId(api.getOwnerId())
                .name(api.getName())
                .slug(api.getSlug())
                .description(api.getDescription())
                .category(api.getCategory())
                .targetBackendUrl(api.getTargetBackendUrl())
                .status(api.getStatus())
                .versionLabel(api.getVersionLabel())
                .isPublic(api.isPublic())
                .createdAt(api.getCreatedAt())
                .updatedAt(api.getUpdatedAt())
                .endpoints(endpoints)
                .plans(plans)
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

    private InternalApiResolutionResponse mapToInternalResolution(Api api) {
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
}
