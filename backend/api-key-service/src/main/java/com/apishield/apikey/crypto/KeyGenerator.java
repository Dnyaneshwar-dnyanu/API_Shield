package com.apishield.apikey.crypto;

import com.apishield.common.util.CryptoUtils;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class KeyGenerator {

    private static final String ALPHANUMERIC = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String PREFIX_CHARS = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final int PREFIX_LENGTH = 8;
    private static final int SECRET_LENGTH = 32;
    private static final String KEY_CONSTANT_PREFIX = "ash_live_";

    private final SecureRandom secureRandom = new SecureRandom();

    public record GeneratedKey(String rawKey, String keyPrefix, String keyHash) {}

    public GeneratedKey generate() {
        String randomPrefix = generateRandomString(PREFIX_CHARS, PREFIX_LENGTH);
        String randomSecret = generateRandomString(ALPHANUMERIC, SECRET_LENGTH);

        String fullPrefix = KEY_CONSTANT_PREFIX + randomPrefix;
        String rawKey = fullPrefix + "_" + randomSecret;
        String keyHash = CryptoUtils.sha256Hex(rawKey);

        return new GeneratedKey(rawKey, fullPrefix, keyHash);
    }

    private String generateRandomString(String charPool, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = secureRandom.nextInt(charPool.length());
            sb.append(charPool.charAt(index));
        }
        return sb.toString();
    }
}
