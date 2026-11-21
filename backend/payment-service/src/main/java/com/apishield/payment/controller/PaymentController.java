package com.apishield.payment.controller;

import com.apishield.common.constant.HeaderConstants;
import com.apishield.payment.dto.*;
import com.apishield.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    public ResponseEntity<CreateOrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = HeaderConstants.X_USER_ID, required = false) String developerId) {
        log.info("REST: create payment order for subscription: {}", request.getSubscriptionId());
        CreateOrderResponse response = paymentService.createOrder(request, developerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request,
            @RequestHeader(value = HeaderConstants.X_USER_ID, required = false) String developerId) {
        log.info("REST: verify payment signature for order: {}", request.getOrderId());
        PaymentResponse response = paymentService.verifyPayment(request, developerId);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> handleWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        log.info("REST: received Razorpay webhook callback");
        Map<String, Object> result = paymentService.processWebhook(rawPayload, signature);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/history")
    public ResponseEntity<List<PaymentResponse>> getPaymentHistory(
            @RequestHeader(value = HeaderConstants.X_USER_ID, required = false) String developerId) {
        log.info("REST: get payment history for developer: {}", developerId);
        if (developerId == null || developerId.isBlank()) {
            throw new IllegalArgumentException("X-User-Id header is required to retrieve payment history");
        }
        return ResponseEntity.ok(paymentService.getHistory(developerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @PathVariable UUID id,
            @RequestHeader(value = HeaderConstants.X_USER_ID, required = false) String developerId) {
        log.info("REST: get payment details for ID: {}", id);
        return ResponseEntity.ok(paymentService.getPaymentById(id, developerId));
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<InvoiceResponse> getInvoiceByPaymentId(
            @PathVariable UUID id,
            @RequestHeader(value = HeaderConstants.X_USER_ID, required = false) String developerId) {
        log.info("REST: get invoice for payment ID: {}", id);
        return ResponseEntity.ok(paymentService.getInvoiceByPaymentId(id, developerId));
    }
}
