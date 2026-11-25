package com.apishield.registry.controller;

import com.apishield.common.constant.HeaderConstants;
import com.apishield.registry.dto.*;
import com.apishield.registry.service.ApiRegistryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/apis")
@RequiredArgsConstructor
public class ApiController {

    private final ApiRegistryService registryService;

    @PostMapping
    public ResponseEntity<ApiResponse> createApi(
            @Valid @RequestBody CreateApiRequest request,
            @RequestHeader(value = HeaderConstants.X_USER_ID, defaultValue = "owner-default-id") String ownerId) {
        ApiResponse response = registryService.createApi(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ApiResponse>> getMyApis(
            @RequestHeader(value = HeaderConstants.X_USER_ID, defaultValue = "owner-default-id") String ownerId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ApiResponse> response = registryService.getApisByOwner(ownerId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiDetailResponse> getApi(
            @PathVariable UUID id,
            @RequestHeader(value = HeaderConstants.X_USER_ID, defaultValue = "owner-default-id") String ownerId) {
        ApiDetailResponse response = registryService.getApiById(id, ownerId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateApi(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateApiRequest request,
            @RequestHeader(value = HeaderConstants.X_USER_ID, defaultValue = "owner-default-id") String ownerId) {
        ApiResponse response = registryService.updateApi(id, request, ownerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/endpoints")
    public ResponseEntity<EndpointResponse> addEndpoint(
            @PathVariable UUID id,
            @Valid @RequestBody CreateEndpointRequest request,
            @RequestHeader(value = HeaderConstants.X_USER_ID, defaultValue = "owner-default-id") String ownerId) {
        EndpointResponse response = registryService.addEndpoint(id, request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/plans")
    public ResponseEntity<PlanResponse> addPlan(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePlanRequest request,
            @RequestHeader(value = HeaderConstants.X_USER_ID, defaultValue = "owner-default-id") String ownerId) {
        PlanResponse response = registryService.addPlan(id, request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @RequestMapping(value = "/{id}/publish", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ApiResponse> publishApi(
            @PathVariable UUID id,
            @RequestHeader(value = HeaderConstants.X_USER_ID, defaultValue = "owner-default-id") String ownerId) {
        ApiResponse response = registryService.publishApi(id, ownerId);
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = {"/{id}/deprecate", "/{id}/unpublish"}, method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ApiResponse> deprecateApi(
            @PathVariable UUID id,
            @RequestHeader(value = HeaderConstants.X_USER_ID, defaultValue = "owner-default-id") String ownerId) {
        ApiResponse response = registryService.deprecateApi(id, ownerId);
        return ResponseEntity.ok(response);
    }
}
