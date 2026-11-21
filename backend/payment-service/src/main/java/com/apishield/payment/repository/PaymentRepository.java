package com.apishield.payment.repository;

import com.apishield.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(String orderId);

    Optional<Payment> findByPaymentId(String paymentId);

    List<Payment> findByDeveloperIdOrderByCreatedAtDesc(String developerId);

    List<Payment> findBySubscriptionIdOrderByCreatedAtDesc(String subscriptionId);
}
