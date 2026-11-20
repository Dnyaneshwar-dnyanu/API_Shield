package com.apishield.registry.repository;

import com.apishield.registry.entity.ApiPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApiPlanRepository extends JpaRepository<ApiPlan, UUID> {

    List<ApiPlan> findByApiId(UUID apiId);

    List<ApiPlan> findByApiIdAndIsActiveTrue(UUID apiId);
}
