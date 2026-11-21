package com.apishield.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Lightweight cryptographic utility for SHA-256 digests, HMAC-SHA256 signatures, and constant-time comparisons.
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
     * Computes the lowercase hexadecimal HMAC-SHA256 digest of a UTF-8 string with the provided secret.
     *
     * @param data Raw data string
     * @param secret Key secret
     * @return Hex-encoded 64-character HMAC-SHA256 digest, or null if either parameter is null
     */
    public static String hmacSha256Hex(String data, String secret) {
        if (data == null || secret == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hashBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256", e);
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
