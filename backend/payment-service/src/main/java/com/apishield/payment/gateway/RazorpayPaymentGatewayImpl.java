package com.apishield.payment.gateway;

import com.apishield.common.util.CryptoUtils;
import com.apishield.payment.exception.PaymentProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class RazorpayPaymentGatewayImpl implements PaymentGatewayService {

    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;
    private final boolean mockMode;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public RazorpayPaymentGatewayImpl(
            @Value("${razorpay.key-id}") String keyId,
            @Value("${razorpay.key-secret}") String keySecret,
            @Value("${razorpay.webhook-secret}") String webhookSecret,
            @Value("${razorpay.mock:true}") boolean mockMode,
            ObjectMapper objectMapper) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
        this.mockMode = mockMode;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.razorpay.com/v1")
                .build();
    }

    @Override
    public GatewayOrder createOrder(int amountCents, String currency, String receipt, Map<String, String> notes) {
        if (mockMode) {
            String mockOrderId = "order_rzp_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
            log.info("[MOCK] Generated Razorpay Order: {} for amount: {} {}", mockOrderId, amountCents, currency);
            return new GatewayOrder(mockOrderId, amountCents, currency);
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("amount", amountCents);
            body.put("currency", currency);
            body.put("receipt", receipt);
            if (notes != null && !notes.isEmpty()) {
                body.put("notes", notes);
            }

            String authHeader = "Basic " + Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));

            String responseJson = restClient.post()
                    .uri("/orders")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseJson);
            String orderId = root.path("id").asText();
            int amount = root.path("amount").asInt();
            String resCurrency = root.path("currency").asText();

            log.info("Created Live Razorpay Order: {} for amount: {}", orderId, amount);
            return new GatewayOrder(orderId, amount, resCurrency);
        } catch (Exception e) {
            log.error("Failed to create Razorpay live order: {}", e.getMessage());
            throw new PaymentProcessingException("Failed to create Razorpay payment order: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifySignature(String orderId, String paymentId, String signature) {
        if (orderId == null || paymentId == null || signature == null) {
            return false;
        }
        String payload = orderId + "|" + paymentId;
        String expectedSignature = CryptoUtils.hmacSha256Hex(payload, keySecret);
        return CryptoUtils.constantTimeEquals(expectedSignature, signature);
    }

    @Override
    public boolean verifyWebhookSignature(String rawPayload, String signature) {
        if (rawPayload == null || signature == null) {
            return false;
        }
        String expectedSignature = CryptoUtils.hmacSha256Hex(rawPayload, webhookSecret);
        return CryptoUtils.constantTimeEquals(expectedSignature, signature);
    }

    @Override
    public String getKeyId() {
        return keyId;
    }
}
