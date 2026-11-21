package com.apishield.payment.repository;

import com.apishield.payment.entity.WebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, UUID> {

    Optional<WebhookLog> findByEventId(String eventId);

    boolean existsByEventId(String eventId);
}
