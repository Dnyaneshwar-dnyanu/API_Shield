package com.apishield.notification.exception;

import com.apishield.common.constant.HeaderConstants;
import com.apishield.common.dto.ProblemDetailResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetailResponse> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        String correlationId = request.getHeader(HeaderConstants.X_CORRELATION_ID);
        log.warn("Handled ResponseStatusException [{}]: {}", status, ex.getReason());

        ProblemDetailResponse problem = ProblemDetailResponse.builder()
                .type("urn:apishield:error:" + status.value())
                .title(status.getReasonPhrase())
                .status(status.value())
                .detail(ex.getReason() != null ? ex.getReason() : ex.getMessage())
                .instance(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetailResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        String correlationId = request.getHeader(HeaderConstants.X_CORRELATION_ID);
        log.warn("Validation error on request [{}]: {}", request.getRequestURI(), errors);

        ProblemDetailResponse problem = ProblemDetailResponse.builder()
                .type("urn:apishield:error:400")
                .title("Bad Request")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail("Validation failed for one or more request arguments")
                .instance(request.getRequestURI())
                .correlationId(correlationId)
                .errors(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ProblemDetailResponse> handleMissingHeaderException(
            MissingRequestHeaderException ex,
            HttpServletRequest request
    ) {
        String correlationId = request.getHeader(HeaderConstants.X_CORRELATION_ID);
        log.warn("Missing required header: {}", ex.getHeaderName());

        ProblemDetailResponse problem = ProblemDetailResponse.builder()
                .type("urn:apishield:error:400")
                .title("Bad Request")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail(String.format("Required request header '%s' is not present", ex.getHeaderName()))
                .instance(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetailResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        String correlationId = request.getHeader(HeaderConstants.X_CORRELATION_ID);
        log.error("Unhandled exception processing request [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

        ProblemDetailResponse problem = ProblemDetailResponse.builder()
                .type("urn:apishield:error:500")
                .title("Internal Server Error")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail("An unexpected error occurred while processing the request")
                .instance(request.getRequestURI())
                .correlationId(correlationId)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
