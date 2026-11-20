package com.apishield.registry.exception;

/**
 * Thrown when a target backend URL violates SSRF defense rules.
 */
public class SsrfSecurityException extends RuntimeException {

    public SsrfSecurityException(String message) {
        super(message);
    }
}
