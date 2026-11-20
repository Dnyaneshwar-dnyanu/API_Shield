package com.apishield.registry.service;

import com.apishield.registry.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ApiRegistryService {

    ApiResponse createApi(CreateApiRequest request, String ownerId);

    ApiDetailResponse getApiById(UUID id, String ownerId);

    Page<ApiResponse> getApisByOwner(String ownerId, Pageable pageable);

    ApiResponse updateApi(UUID id, UpdateApiRequest request, String ownerId);

    EndpointResponse addEndpoint(UUID apiId, CreateEndpointRequest request, String ownerId);

    PlanResponse addPlan(UUID apiId, CreatePlanRequest request, String ownerId);

    ApiResponse publishApi(UUID id, String ownerId);

    ApiResponse deprecateApi(UUID id, String ownerId);
}
