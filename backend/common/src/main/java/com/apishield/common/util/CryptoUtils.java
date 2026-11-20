package com.apishield.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Lightweight cryptographic utility for SHA-256 digests and constant-time comparisons.
 */
public final class CryptoUtils {

    private CryptoUtils() {
        // Prevent instantiation
    }

    /**
     * Computes the lowercase hexadecimal SHA-256 hash of a UTF-8 string.
     *
     * @param input Raw plaintext string
     * @return Hex-encoded 64-character SHA-256 digest, or null if input is null
     */
    public static String sha256Hex(String input) {
        if (input == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in current JVM", e);
        }
    }

    /**
     * Performs a constant-time comparison of two strings to prevent timing attacks.
     *
     * @param a First string
     * @param b Second string
     * @return true if both strings are non-null and equal; false otherwise
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }
}
