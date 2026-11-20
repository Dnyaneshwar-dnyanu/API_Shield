package com.apishield.registry.exception;

import com.apishield.common.constant.HeaderConstants;
import com.apishield.common.dto.ProblemDetailResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetailResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        String correlationId = extractCorrelationId(request);
        ProblemDetailResponse response = ProblemDetailResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "Resource Not Found",
                ex.getMessage(),
                request.getRequestURI(),
                correlationId
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetailResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        String correlationId = extractCorrelationId(request);
        ProblemDetailResponse response = ProblemDetailResponse.of(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                request.getRequestURI(),
                correlationId
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(SsrfSecurityException.class)
    public ResponseEntity<ProblemDetailResponse> handleSsrf(SsrfSecurityException ex, HttpServletRequest request) {
        String correlationId = extractCorrelationId(request);
        log.warn("Blocked SSRF attempt: {} on URI: {}", ex.getMessage(), request.getRequestURI());
        ProblemDetailResponse response = ProblemDetailResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "SSRF Security Violation",
                ex.getMessage(),
                request.getRequestURI(),
                correlationId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetailResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String correlationId = extractCorrelationId(request);
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ProblemDetailResponse response = ProblemDetailResponse.validationError(
                "Request validation failed",
                request.getRequestURI(),
                correlationId,
                errors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetailResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        String correlationId = extractCorrelationId(request);
        ProblemDetailResponse response = ProblemDetailResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI(),
                correlationId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetailResponse> handleNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex, HttpServletRequest request) {
        String correlationId = extractCorrelationId(request);
        ProblemDetailResponse response = ProblemDetailResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Malformed Request",
                "Failed to parse request payload: " + ex.getMessage(),
                request.getRequestURI(),
                correlationId
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetailResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        String correlationId = extractCorrelationId(request);
        log.error("Unhandled exception processing request: {}", request.getRequestURI(), ex);
        ProblemDetailResponse response = ProblemDetailResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected internal error occurred. Please contact support.",
                request.getRequestURI(),
                correlationId
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String extractCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(HeaderConstants.X_CORRELATION_ID);
        return (correlationId != null && !correlationId.isBlank()) ? correlationId : UUID.randomUUID().toString();
    }
}
