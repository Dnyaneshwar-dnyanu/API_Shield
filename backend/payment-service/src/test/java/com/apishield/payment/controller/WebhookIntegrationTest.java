package com.apishield.payment.controller;

import com.apishield.common.util.CryptoUtils;
import com.apishield.payment.config.TestPaymentConfig;
import com.apishield.payment.entity.Payment;
import com.apishield.payment.model.PaymentStatus;
import com.apishield.payment.repository.InvoiceRepository;
import com.apishield.payment.repository.PaymentRepository;
import com.apishield.payment.repository.WebhookLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestPaymentConfig.class)
class WebhookIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private WebhookLogRepository webhookLogRepository;

    @Value("${razorpay.webhook-secret:rzp_test_mock_webhook_secret}")
    private String webhookSecret;

    @BeforeEach
    void setUp() {
        webhookLogRepository.deleteAll();
        invoiceRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("Should successfully process authentic Razorpay order.paid webhook")
    void shouldProcessOrderPaidWebhook() throws Exception {
        String orderId = "order_test_hook_001";
        Payment payment = Payment.builder()
                .subscriptionId(UUID.randomUUID().toString())
                .developerId(UUID.randomUUID().toString())
                .planId(UUID.randomUUID().toString())
                .provider("RAZORPAY")
                .orderId(orderId)
                .amountCents(3500)
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        String rawPayload = """
                {
                  "id": "evt_hook_test_001",
                  "event": "order.paid",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_hook_001",
                        "order_id": "%s",
                        "amount": 3500,
                        "currency": "INR",
                        "status": "captured"
                      }
                    }
                  }
                }
                """.formatted(orderId);

        String signature = CryptoUtils.hmacSha256Hex(rawPayload, webhookSecret);

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(rawPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.event", is("order.paid")));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(updated.getPaymentId()).isEqualTo("pay_hook_001");

        // Verify WebhookLog recorded
        assertThat(webhookLogRepository.findByEventId("evt_hook_test_001")).isPresent();
        assertThat(webhookLogRepository.findByEventId("evt_hook_test_001").get().isProcessed()).isTrue();

        // Verify Invoice issued
        assertThat(invoiceRepository.findByPaymentId(updated.getId().toString())).isPresent();
    }

    @Test
    @DisplayName("Should reject webhook with invalid signature")
    void shouldRejectInvalidWebhookSignature() throws Exception {
        String rawPayload = "{\"id\":\"evt_fake\",\"event\":\"order.paid\"}";

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", "invalid_signature_hex")
                        .content(rawPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Invalid Cryptographic Signature")));
    }

    @Test
    @DisplayName("Should ignore duplicate webhook replay idempotently")
    void shouldIgnoreDuplicateWebhook() throws Exception {
        String rawPayload = """
                {
                  "id": "evt_duplicate_001",
                  "event": "order.paid",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_dup_001",
                        "order_id": "order_nonexistent",
                        "amount": 1000
                      }
                    }
                  }
                }
                """;
        String signature = CryptoUtils.hmacSha256Hex(rawPayload, webhookSecret);

        // First call
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(rawPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")));

        // Second duplicate call
        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(rawPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ignored")))
                .andExpect(jsonPath("$.reason", is("duplicate_event")));
    }

    @Test
    @DisplayName("Should process payment.failed webhook and mark payment FAILED")
    void shouldProcessPaymentFailedWebhook() throws Exception {
        String orderId = "order_test_hook_fail";
        Payment payment = Payment.builder()
                .subscriptionId(UUID.randomUUID().toString())
                .developerId(UUID.randomUUID().toString())
                .planId(UUID.randomUUID().toString())
                .provider("RAZORPAY")
                .orderId(orderId)
                .amountCents(5000)
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        String rawPayload = """
                {
                  "id": "evt_hook_fail_001",
                  "event": "payment.failed",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_fail_001",
                        "order_id": "%s",
                        "error_description": "Card was declined by issuing bank"
                      }
                    }
                  }
                }
                """.formatted(orderId);

        String signature = CryptoUtils.hmacSha256Hex(rawPayload, webhookSecret);

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", signature)
                        .content(rawPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.event", is("payment.failed")));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(updated.getFailureReason()).isEqualTo("Card was declined by issuing bank");
    }
}
