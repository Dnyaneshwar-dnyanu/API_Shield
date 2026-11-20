package com.apishield.common.constant;

/**
 * Standard HTTP header constants used for inter-service communication,
 * distributed request tracing, and security propagation.
 */
public final class HeaderConstants {

    private HeaderConstants() {
        // Prevent instantiation
    }

    /**
     * Unique request correlation identifier propagated across all microservices and Gateway.
     */
    public static final String X_CORRELATION_ID = "X-Correlation-ID";

    /**
     * Internal cryptographic HMAC token validating that a request originated from the API Gateway.
     */
    public static final String X_INTERNAL_TOKEN = "X-Internal-Token";

    /**
     * Public API key passed by API consumers on Data Plane proxy routes.
     */
    public static final String X_API_KEY = "X-API-Key";

    /**
     * Authenticated user identifier forwarded by the API Gateway to downstream services.
     */
    public static final String X_USER_ID = "X-User-Id";

    /**
     * Authenticated user email address forwarded by the API Gateway.
     */
    public static final String X_USER_EMAIL = "X-User-Email";

    /**
     * Authenticated user roles (e.g. ROLE_DEVELOPER, ROLE_OWNER, ROLE_ADMIN) forwarded by the API Gateway.
     */
    public static final String X_USER_ROLE = "X-User-Role";
}
