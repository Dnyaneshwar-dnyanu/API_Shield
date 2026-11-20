package com.apishield.registry.service;

import com.apishield.registry.dto.InternalApiResolutionResponse;
import com.apishield.registry.dto.PlanResponse;

import java.util.UUID;

public interface InternalResolutionService {

    InternalApiResolutionResponse resolveApiBySlug(String slug);

    PlanResponse getPlanById(UUID planId);
}
