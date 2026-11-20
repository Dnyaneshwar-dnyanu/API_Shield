package com.apishield.common.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Should create standard problem detail response")
    void shouldCreateProblemDetail() throws Exception {
        ProblemDetailResponse response = ProblemDetailResponse.of(
                404,
                "Not Found",
                "User with id 123 was not found",
                "/api/v1/users/123",
                "corr-999"
        );

        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getTitle()).isEqualTo("Not Found");
        assertThat(response.getDetail()).isEqualTo("User with id 123 was not found");
        assertThat(response.getInstance()).isEqualTo("/api/v1/users/123");
        assertThat(response.getCorrelationId()).isEqualTo("corr-999");
        assertThat(response.getTimestamp()).isNotNull();

        String json = objectMapper.writeValueAsString(response);
        assertThat(json).contains("Not Found").contains("corr-999");
    }

    @Test
    @DisplayName("Should create validation error problem detail")
    void shouldCreateValidationErrorProblemDetail() {
        Map<String, String> errors = Map.of("email", "Invalid email format");
        ProblemDetailResponse response = ProblemDetailResponse.validationError(
                "Validation failed for request",
                "/api/v1/auth/register",
                "corr-111",
                errors
        );

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getErrors()).containsEntry("email", "Invalid email format");
    }
}
