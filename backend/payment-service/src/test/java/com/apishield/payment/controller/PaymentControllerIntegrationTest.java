package com.apishield.payment.controller;

import com.apishield.common.constant.HeaderConstants;
import com.apishield.common.util.CryptoUtils;
import com.apishield.payment.client.RegistryClient;
import com.apishield.payment.client.SubscriptionClient;
import com.apishield.payment.config.TestPaymentConfig;
import com.apishield.payment.dto.CreateOrderRequest;
import com.apishield.payment.dto.VerifyPaymentRequest;
import com.apishield.payment.dto.client.PlanClientResponse;
import com.apishield.payment.dto.client.SubscriptionClientResponse;
import com.apishield.payment.entity.Payment;
import com.apishield.payment.model.PaymentStatus;
import com.apishield.payment.repository.InvoiceRepository;
import com.apishield.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestPaymentConfig.class)
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private SubscriptionClient subscriptionClient;

    @Autowired
    private RegistryClient registryClient;

    @Value("${razorpay.key-secret:rzp_test_mock_secret}")
    private String keySecret;

    private final String devId = UUID.randomUUID().toString();
    private final String planId = UUID.randomUUID().toString();
    private final String subId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
        paymentRepository.deleteAll();
    }

    @Test
    @DisplayName("Should successfully create a payment order for a pending subscription")
    void shouldCreatePaymentOrder() throws Exception {
        SubscriptionClientResponse subResponse = SubscriptionClientResponse.builder()
                .id(UUID.fromString(subId))
                .developerId(devId)
                .apiId(UUID.randomUUID().toString())
                .planId(planId)
                .status("PENDING")
                .build();

        PlanClientResponse planResponse = PlanClientResponse.builder()
                .id(UUID.fromString(planId))
                .name("Pro Tier")
                .priceCents(4999)
                .billingCycle("MONTHLY")
                .isActive(true)
                .build();

        Mockito.when(subscriptionClient.getSubscriptionById(subId)).thenReturn(Optional.of(subResponse));
        Mockito.when(registryClient.getPlanById(planId)).thenReturn(Optional.of(planResponse));

        CreateOrderRequest request = CreateOrderRequest.builder()
                .subscriptionId(subId)
                .build();

        mockMvc.perform(post("/api/v1/payments/create-order")
                        .header(HeaderConstants.X_USER_ID, devId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId", startsWith("order_rzp_mock_")))
                .andExpect(jsonPath("$.amountCents", is(4999)))
                .andExpect(jsonPath("$.currency", is("INR")))
                .andExpect(jsonPath("$.paymentId", notNullValue()));

        assertThat(paymentRepository.findAll()).hasSize(1);
        Payment saved = paymentRepository.findAll().get(0);
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(saved.getAmountCents()).isEqualTo(4999);
    }

    @Test
    @DisplayName("Should return 404 when subscription is not found")
    void shouldReturn404WhenSubscriptionNotFound() throws Exception {
        Mockito.when(subscriptionClient.getSubscriptionById("sub-nonexistent")).thenReturn(Optional.empty());

        CreateOrderRequest request = CreateOrderRequest.builder()
                .subscriptionId("sub-nonexistent")
                .build();

        mockMvc.perform(post("/api/v1/payments/create-order")
                        .header(HeaderConstants.X_USER_ID, devId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title", is("Resource Not Found")));
    }

    @Test
    @DisplayName("Should return 409 Conflict when subscription is already ACTIVE")
    void shouldReturn409WhenSubscriptionAlreadyActive() throws Exception {
        SubscriptionClientResponse subResponse = SubscriptionClientResponse.builder()
                .id(UUID.fromString(subId))
                .developerId(devId)
                .planId(planId)
                .status("ACTIVE")
                .build();

        Mockito.when(subscriptionClient.getSubscriptionById(subId)).thenReturn(Optional.of(subResponse));

        CreateOrderRequest request = CreateOrderRequest.builder()
                .subscriptionId(subId)
                .build();

        mockMvc.perform(post("/api/v1/payments/create-order")
                        .header(HeaderConstants.X_USER_ID, devId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("Conflict")));
    }

    @Test
    @DisplayName("Should verify valid payment signature, mark COMPLETED, and issue invoice")
    void shouldVerifyValidPaymentSignature() throws Exception {
        String orderId = "order_rzp_mock_test123";
        Payment payment = Payment.builder()
                .subscriptionId(subId)
                .developerId(devId)
                .planId(planId)
                .provider("RAZORPAY")
                .orderId(orderId)
                .amountCents(2999)
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        String paymentId = "pay_rzp_mock_99999";
        String validSignature = CryptoUtils.hmacSha256Hex(orderId + "|" + paymentId, keySecret);

        VerifyPaymentRequest request = VerifyPaymentRequest.builder()
                .orderId(orderId)
                .paymentId(paymentId)
                .signature(validSignature)
                .build();

        mockMvc.perform(post("/api/v1/payments/verify")
                        .header(HeaderConstants.X_USER_ID, devId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId", is(orderId)))
                .andExpect(jsonPath("$.paymentId", is(paymentId)))
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.COMPLETED);

        // Verify invoice generated
        assertThat(invoiceRepository.findByPaymentId(updated.getId().toString())).isPresent();
    }

    @Test
    @DisplayName("Should reject invalid signature with 400 Bad Request")
    void shouldRejectInvalidSignature() throws Exception {
        String orderId = "order_rzp_mock_test456";
        Payment payment = Payment.builder()
                .subscriptionId(subId)
                .developerId(devId)
                .planId(planId)
                .provider("RAZORPAY")
                .orderId(orderId)
                .amountCents(1999)
                .currency("INR")
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        VerifyPaymentRequest request = VerifyPaymentRequest.builder()
                .orderId(orderId)
                .paymentId("pay_tampered_123")
                .signature("invalid_signature_hex")
                .build();

        mockMvc.perform(post("/api/v1/payments/verify")
                        .header(HeaderConstants.X_USER_ID, devId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Invalid Cryptographic Signature")));
    }

    @Test
    @DisplayName("Should return payment history and invoice for authenticated developer")
    void shouldGetHistoryAndInvoice() throws Exception {
        Payment payment = Payment.builder()
                .subscriptionId(subId)
                .developerId(devId)
                .planId(planId)
                .provider("RAZORPAY")
                .orderId("order_test_hist")
                .paymentId("pay_test_hist")
                .amountCents(1500)
                .currency("INR")
                .status(PaymentStatus.COMPLETED)
                .build();
        payment = paymentRepository.save(payment);

        // Fetch history
        mockMvc.perform(get("/api/v1/payments/history")
                        .header(HeaderConstants.X_USER_ID, devId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].orderId", is("order_test_hist")));

        // Fetch by ID
        mockMvc.perform(get("/api/v1/payments/" + payment.getId())
                        .header(HeaderConstants.X_USER_ID, devId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amountCents", is(1500)));
    }
}
