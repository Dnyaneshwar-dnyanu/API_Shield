package com.apishield.subscription.repository;

import com.apishield.subscription.entity.Subscription;
import com.apishield.subscription.model.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Page<Subscription> findByDeveloperId(String developerId, Pageable pageable);

    Optional<Subscription> findByIdAndDeveloperId(UUID id, String developerId);

    Optional<Subscription> findFirstByDeveloperIdAndApiIdAndStatus(String developerId, String apiId, SubscriptionStatus status);

    boolean existsByDeveloperIdAndApiIdAndStatusIn(String developerId, String apiId, Collection<SubscriptionStatus> statuses);
}
