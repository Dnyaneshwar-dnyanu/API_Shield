package com.apishield.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CryptoUtilsTest {

    @Test
    @DisplayName("Should correctly compute SHA-256 hexadecimal hash")
    void shouldComputeSha256Hex() {
        String input = "test-token-123";
        String expected = "19b6b086eebb807f54e6327309dec0ff347a6c3c30bf3bb396f167513eba3475";

        String result = CryptoUtils.sha256Hex(input);

        assertThat(result).isEqualTo(expected);

        // Also verify standard "hello"
        assertThat(CryptoUtils.sha256Hex("hello"))
                .isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }

    @Test
    @DisplayName("Should return null when input is null")
    void shouldReturnNullForNullInput() {
        assertThat(CryptoUtils.sha256Hex(null)).isNull();
    }

    @Test
    @DisplayName("Should verify constant-time equality correctly")
    void shouldVerifyConstantTimeEquals() {
        assertThat(CryptoUtils.constantTimeEquals("secret123", "secret123")).isTrue();
        assertThat(CryptoUtils.constantTimeEquals("secret123", "secret456")).isFalse();
        assertThat(CryptoUtils.constantTimeEquals(null, "secret123")).isFalse();
        assertThat(CryptoUtils.constantTimeEquals("secret123", null)).isFalse();
        assertThat(CryptoUtils.constantTimeEquals(null, null)).isFalse();
    }

    @Test
    @DisplayName("Should correctly compute HMAC-SHA256 hex digest")
    void shouldComputeHmacSha256Hex() {
        String data = "order_12345|pay_67890";
        String secret = "rzp_secret_key";
        String hex = CryptoUtils.hmacSha256Hex(data, secret);

        assertThat(hex).isNotNull().hasSize(64);
        // Verify deterministic calculation
        assertThat(CryptoUtils.hmacSha256Hex(data, secret)).isEqualTo(hex);
        // Verify null handling
        assertThat(CryptoUtils.hmacSha256Hex(null, secret)).isNull();
        assertThat(CryptoUtils.hmacSha256Hex(data, null)).isNull();
    }
}
