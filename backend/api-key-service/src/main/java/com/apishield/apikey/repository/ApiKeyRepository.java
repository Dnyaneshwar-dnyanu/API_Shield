package com.apishield.apikey.repository;

import com.apishield.apikey.entity.ApiKey;
import com.apishield.apikey.model.KeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    Optional<ApiKey> findByKeyHash(String keyHash);

    List<ApiKey> findByDeveloperIdOrderByCreatedAtDesc(String developerId);

    List<ApiKey> findBySubscriptionId(String subscriptionId);

    List<ApiKey> findBySubscriptionIdAndStatus(String subscriptionId, KeyStatus status);

    long countBySubscriptionIdAndStatus(String subscriptionId, KeyStatus status);
}
