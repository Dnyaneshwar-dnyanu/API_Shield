package com.apishield.analytics.exception;

import com.apishield.common.dto.ProblemDetailResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetailResponse> handleResponseStatusException(
            ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        ProblemDetailResponse problem = ProblemDetailResponse.builder()
                .type("urn:apishield:error:analytics")
                .title(status.getReasonPhrase())
                .status(status.value())
                .detail(ex.getReason())
                .instance(request.getRequestURI())
                .build();
        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetailResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        ProblemDetailResponse problem = ProblemDetailResponse.builder()
                .type("urn:apishield:error:validation")
                .title("Validation Failed")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail("One or more fields failed validation")
                .instance(request.getRequestURI())
                .errors(errors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetailResponse> handleGeneralException(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception processing analytics request: {}", ex.getMessage(), ex);
        ProblemDetailResponse problem = ProblemDetailResponse.builder()
                .type("urn:apishield:error:internal")
                .title("Internal Server Error")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail("An unexpected server error occurred in Analytics Service")
                .instance(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
