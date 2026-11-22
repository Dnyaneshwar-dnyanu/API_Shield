package com.apishield.apikey.crypto;

import com.apishield.common.util.CryptoUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeyGeneratorTest {

    private final KeyGenerator keyGenerator = new KeyGenerator();

    @Test
    @DisplayName("Should generate valid API key adhering to standard ash_live_ format")
    void shouldGenerateValidApiKey() {
        KeyGenerator.GeneratedKey generated = keyGenerator.generate();

        assertThat(generated).isNotNull();
        assertThat(generated.rawKey()).isNotBlank();
        assertThat(generated.keyPrefix()).isNotBlank();
        assertThat(generated.keyHash()).isNotBlank();

        // Format checks: ash_live_{prefix}_{secret}
        assertThat(generated.rawKey()).startsWith("ash_live_");
        assertThat(generated.keyPrefix()).startsWith("ash_live_");
        assertThat(generated.keyPrefix()).hasSize(17); // "ash_live_" (9) + 8 chars = 17

        // Secret length: 32 chars -> total raw key length: 17 + 1 ("_") + 32 = 50 chars
        assertThat(generated.rawKey()).hasSize(50);

        // Hash verification
        String expectedHash = CryptoUtils.sha256Hex(generated.rawKey());
        assertThat(generated.keyHash()).isEqualTo(expectedHash);
    }

    @Test
    @DisplayName("Generated keys should have distinct high entropy")
    void shouldGenerateUniqueKeys() {
        KeyGenerator.GeneratedKey key1 = keyGenerator.generate();
        KeyGenerator.GeneratedKey key2 = keyGenerator.generate();

        assertThat(key1.rawKey()).isNotEqualTo(key2.rawKey());
        assertThat(key1.keyPrefix()).isNotEqualTo(key2.keyPrefix());
        assertThat(key1.keyHash()).isNotEqualTo(key2.keyHash());
    }
}
