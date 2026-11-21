package com.apishield.payment.service.impl;

import com.apishield.payment.client.RegistryClient;
import com.apishield.payment.client.SubscriptionClient;
import com.apishield.payment.dto.*;
import com.apishield.payment.dto.client.PlanClientResponse;
import com.apishield.payment.dto.client.SubscriptionClientResponse;
import com.apishield.payment.entity.Invoice;
import com.apishield.payment.entity.Payment;
import com.apishield.payment.entity.WebhookLog;
import com.apishield.payment.exception.ConflictException;
import com.apishield.payment.exception.InvalidSignatureException;
import com.apishield.payment.exception.ResourceNotFoundException;
import com.apishield.payment.gateway.PaymentGatewayService;
import com.apishield.payment.model.InvoiceStatus;
import com.apishield.payment.model.PaymentStatus;
import com.apishield.payment.repository.InvoiceRepository;
import com.apishield.payment.repository.PaymentRepository;
import com.apishield.payment.repository.WebhookLogRepository;
import com.apishield.payment.service.PaymentEventPublisher;
import com.apishield.payment.service.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final WebhookLogRepository webhookLogRepository;
    private final SubscriptionClient subscriptionClient;
    private final RegistryClient registryClient;
    private final PaymentGatewayService paymentGatewayService;
    private final PaymentEventPublisher paymentEventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request, String developerId) {
        log.info("Initiating payment order creation for subscription: {}, developer: {}",
                request.getSubscriptionId(), developerId);

        SubscriptionClientResponse subscription = subscriptionClient.getSubscriptionById(request.getSubscriptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found: " + request.getSubscriptionId()));

        if (developerId != null && !developerId.isBlank() && !developerId.equals(subscription.getDeveloperId())) {
            throw new IllegalArgumentException("Subscription does not belong to the current developer");
        }

        if (!"PENDING".equalsIgnoreCase(subscription.getStatus())) {
            throw new ConflictException("Subscription is not in PENDING state (current: " + subscription.getStatus() + ")");
        }

        PlanClientResponse plan = registryClient.getPlanById(subscription.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + subscription.getPlanId()));

        if (plan.getPriceCents() <= 0) {
            throw new IllegalArgumentException("Cannot initiate checkout order for a free plan (amount must be > 0)");
        }

        Map<String, String> notes = Map.of(
                "subscriptionId", subscription.getId().toString(),
                "developerId", developerId != null ? developerId : subscription.getDeveloperId(),
                "planId", plan.getId().toString()
        );

        PaymentGatewayService.GatewayOrder gatewayOrder = paymentGatewayService.createOrder(
                plan.getPriceCents(),
                "INR",
                "sub_" + subscription.getId().toString().substring(0, 8),
                notes
        );

        Payment payment = Payment.builder()
                .subscriptionId(subscription.getId().toString())
                .developerId(developerId != null ? developerId : subscription.getDeveloperId())
                .planId(plan.getId().toString())
                .provider("RAZORPAY")
                .orderId(gatewayOrder.orderId())
                .amountCents(gatewayOrder.amountCents())
                .currency(gatewayOrder.currency())
                .status(PaymentStatus.PENDING)
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Saved pending payment record: {} for order: {}", saved.getId(), saved.getOrderId());

        return CreateOrderResponse.builder()
                .paymentId(saved.getId())
                .orderId(saved.getOrderId())
                .amountCents(saved.getAmountCents())
                .currency(saved.getCurrency())
                .keyId(paymentGatewayService.getKeyId())
                .build();
    }

    @Override
    @Transactional
    public PaymentResponse verifyPayment(VerifyPaymentRequest request, String developerId) {
        log.info("Verifying client payment signature for order: {}, payment: {}",
                request.getOrderId(), request.getPaymentId());

        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found: " + request.getOrderId()));

        if (developerId != null && !developerId.isBlank() && !developerId.equals(payment.getDeveloperId())) {
            throw new IllegalArgumentException("Payment does not belong to the authenticated developer");
        }

        // Idempotency check: if already completed, return existing response
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            log.info("Payment order {} is already COMPLETED. Returning existing record.", request.getOrderId());
            return mapToResponse(payment);
        }

        boolean isValid = paymentGatewayService.verifySignature(
                request.getOrderId(),
                request.getPaymentId(),
                request.getSignature()
        );

        if (!isValid) {
            log.warn("Invalid payment signature for order: {}", request.getOrderId());
            throw new InvalidSignatureException("Razorpay checkout signature verification failed");
        }

        payment.setPaymentId(request.getPaymentId());
        payment.setSignature(request.getSignature());
        payment.setStatus(PaymentStatus.COMPLETED);
        Payment completedPayment = paymentRepository.save(payment);

        createInvoiceIfAbsent(completedPayment);
        paymentEventPublisher.publishPaymentCompleted(completedPayment);

        return mapToResponse(completedPayment);
    }

    @Override
    @Transactional
    public Map<String, Object> processWebhook(String rawPayload, String signature) {
        log.info("Processing Razorpay webhook callback");

        if (signature == null || signature.isBlank()) {
            throw new InvalidSignatureException("Missing X-Razorpay-Signature header");
        }

        boolean isValid = paymentGatewayService.verifyWebhookSignature(rawPayload, signature);
        if (!isValid) {
            log.warn("Invalid Razorpay webhook signature");
            throw new InvalidSignatureException("Razorpay webhook HMAC signature verification failed");
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String eventId = root.path("id").asText(null);
            if (eventId == null || eventId.isBlank()) {
                eventId = "evt_" + UUID.nameUUIDFromBytes(rawPayload.getBytes()).toString();
            }

            String eventType = root.path("event").asText("");

            if (webhookLogRepository.existsByEventId(eventId)) {
                log.info("Webhook event {} has already been processed. Skipping duplicate.", eventId);
                return Map.of("status", "ignored", "reason", "duplicate_event", "eventId", eventId);
            }

            WebhookLog logRecord = WebhookLog.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .payload(rawPayload)
                    .signature(signature)
                    .processed(false)
                    .build();
            logRecord = webhookLogRepository.save(logRecord);

            if ("order.paid".equalsIgnoreCase(eventType) || "payment.captured".equalsIgnoreCase(eventType)) {
                handleSuccessfulWebhookEvent(root);
            } else if ("payment.failed".equalsIgnoreCase(eventType)) {
                handleFailedWebhookEvent(root);
            } else {
                log.info("Unhandled webhook event type: {}", eventType);
            }

            logRecord.setProcessed(true);
            logRecord.setProcessedAt(Instant.now());
            webhookLogRepository.save(logRecord);

            return Map.of("status", "success", "event", eventType, "eventId", eventId);
        } catch (InvalidSignatureException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to process webhook payload: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing webhook: " + e.getMessage(), e);
        }
    }

    private void handleSuccessfulWebhookEvent(JsonNode root) {
        JsonNode entity = root.path("payload").path("payment").path("entity");
        if (entity.isMissingNode()) {
            entity = root.path("payload").path("order").path("entity");
        }

        String orderId = entity.path("order_id").asText(null);
        String paymentId = entity.path("id").asText(null);

        if (orderId != null && !orderId.isBlank()) {
            Optional<Payment> optionalPayment = paymentRepository.findByOrderId(orderId);
            if (optionalPayment.isPresent()) {
                Payment payment = optionalPayment.get();
                if (payment.getStatus() == PaymentStatus.PENDING) {
                    payment.setStatus(PaymentStatus.COMPLETED);
                    if (paymentId != null && !paymentId.isBlank()) {
                        payment.setPaymentId(paymentId);
                    }
                    Payment saved = paymentRepository.save(payment);
                    createInvoiceIfAbsent(saved);
                    paymentEventPublisher.publishPaymentCompleted(saved);
                    log.info("Webhook marked payment {} as COMPLETED for order {}", saved.getId(), orderId);
                }
            }
        }
    }

    private void handleFailedWebhookEvent(JsonNode root) {
        JsonNode entity = root.path("payload").path("payment").path("entity");
        String orderId = entity.path("order_id").asText(null);
        String reason = entity.path("error_description").asText("Payment failed");

        if (orderId != null && !orderId.isBlank()) {
            Optional<Payment> optionalPayment = paymentRepository.findByOrderId(orderId);
            if (optionalPayment.isPresent()) {
                Payment payment = optionalPayment.get();
                if (payment.getStatus() == PaymentStatus.PENDING) {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setFailureReason(reason);
                    Payment saved = paymentRepository.save(payment);
                    paymentEventPublisher.publishPaymentFailed(saved, reason);
                    log.warn("Webhook marked payment {} as FAILED for order {}", saved.getId(), orderId);
                }
            }
        }
    }

    private void createInvoiceIfAbsent(Payment payment) {
        if (invoiceRepository.findByPaymentId(payment.getId().toString()).isEmpty()) {
            String datePrefix = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now());
            String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
            String invoiceNumber = "INV-" + datePrefix + "-" + randomPart;

            Invoice invoice = Invoice.builder()
                    .paymentId(payment.getId().toString())
                    .developerId(payment.getDeveloperId())
                    .invoiceNumber(invoiceNumber)
                    .amountCents(payment.getAmountCents())
                    .currency(payment.getCurrency())
                    .status(InvoiceStatus.PAID)
                    .billingPeriodStart(Instant.now())
                    .billingPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS))
                    .build();

            invoiceRepository.save(invoice);
            log.info("Generated invoice {} for payment {}", invoiceNumber, payment.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getHistory(String developerId) {
        return paymentRepository.findByDeveloperIdOrderByCreatedAtDesc(developerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID id, String developerId) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));

        if (developerId != null && !developerId.isBlank() && !developerId.equals(payment.getDeveloperId())) {
            throw new IllegalArgumentException("Payment does not belong to the current developer");
        }

        return mapToResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceByPaymentId(UUID paymentId, String developerId) {
        Invoice invoice = invoiceRepository.findByPaymentId(paymentId.toString())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found for payment: " + paymentId));

        if (developerId != null && !developerId.isBlank() && !developerId.equals(invoice.getDeveloperId())) {
            throw new IllegalArgumentException("Invoice does not belong to the current developer");
        }

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .paymentId(invoice.getPaymentId())
                .developerId(invoice.getDeveloperId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .amountCents(invoice.getAmountCents())
                .currency(invoice.getCurrency())
                .status(invoice.getStatus())
                .billingPeriodStart(invoice.getBillingPeriodStart())
                .billingPeriodEnd(invoice.getBillingPeriodEnd())
                .createdAt(invoice.getCreatedAt())
                .build();
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .subscriptionId(payment.getSubscriptionId())
                .developerId(payment.getDeveloperId())
                .planId(payment.getPlanId())
                .provider(payment.getProvider())
                .orderId(payment.getOrderId())
                .paymentId(payment.getPaymentId())
                .amountCents(payment.getAmountCents())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
