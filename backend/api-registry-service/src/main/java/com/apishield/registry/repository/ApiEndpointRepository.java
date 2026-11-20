package com.apishield.registry.repository;

import com.apishield.registry.entity.ApiEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApiEndpointRepository extends JpaRepository<ApiEndpoint, UUID> {

    List<ApiEndpoint> findByApiId(UUID apiId);

    List<ApiEndpoint> findByApiIdAndIsActiveTrue(UUID apiId);
}
