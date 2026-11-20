package com.apishield.registry.security;

import com.apishield.registry.exception.SsrfSecurityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SsrfProtectionValidatorTest {

    private SsrfProtectionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SsrfProtectionValidator();
    }

    @Test
    @DisplayName("Should reject null and blank target URLs")
    void shouldRejectNullAndBlankUrls() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(SsrfSecurityException.class)
                .hasMessageContaining("cannot be empty");

        assertThatThrownBy(() -> validator.validate("   "))
                .isInstanceOf(SsrfSecurityException.class)
                .hasMessageContaining("cannot be empty");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://api.example.com",
            "file:///etc/passwd",
            "gopher://evil.com/1",
            "ssh://git@github.com",
            "ldap://127.0.0.1:389"
    })
    @DisplayName("Should reject non-HTTP/HTTPS protocols")
    void shouldRejectNonHttpProtocols(String url) {
        assertThatThrownBy(() -> validator.validate(url))
                .isInstanceOf(SsrfSecurityException.class)
                .hasMessageContaining("HTTP or HTTPS");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost/api",
            "http://localhost:8080/api",
            "http://127.0.0.1/status",
            "http://127.0.0.1:3000",
            "http://0.0.0.0:8080",
            "http://[::1]:8080"
    })
    @DisplayName("Should reject loopback and local addresses")
    void shouldRejectLoopbackAddresses(String url) {
        assertThatThrownBy(() -> validator.validate(url))
                .isInstanceOf(SsrfSecurityException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://169.254.169.254/latest/meta-data",
            "http://169.254.169.254/",
            "http://169.254.1.1/secret"
    })
    @DisplayName("Should reject AWS/cloud metadata addresses")
    void shouldRejectCloudMetadata(String url) {
        assertThatThrownBy(() -> validator.validate(url))
                .isInstanceOf(SsrfSecurityException.class)
                .hasMessageContaining("metadata");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://user-service:8081/api",
            "http://discovery-server:8761",
            "http://api-registry-service:8082",
            "http://kafka:9092",
            "http://redis:6379",
            "http://mysql:3306",
            "http://grafana:3000"
    })
    @DisplayName("Should reject internal container hostnames")
    void shouldRejectInternalContainerHostnames(String url) {
        assertThatThrownBy(() -> validator.validate(url))
                .isInstanceOf(SsrfSecurityException.class)
                .hasMessageContaining("restricted internal or loopback address");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://api.internal/data",
            "http://service.local/v1",
            "http://mytest.localhost/api"
    })
    @DisplayName("Should reject reserved internal domain suffixes")
    void shouldRejectInternalDomainSuffixes(String url) {
        assertThatThrownBy(() -> validator.validate(url))
                .isInstanceOf(SsrfSecurityException.class)
                .hasMessageContaining("reserved internal domain suffix");
    }

    @Test
    @DisplayName("Should reject embedded user credentials")
    void shouldRejectEmbeddedUserCredentials() {
        assertThatThrownBy(() -> validator.validate("https://user:password@api.example.com/data"))
                .isInstanceOf(SsrfSecurityException.class)
                .hasMessageContaining("User credentials are not permitted");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://10.0.0.1/api",
            "http://10.255.255.255/test",
            "http://192.168.1.1/admin",
            "http://172.16.0.1/data",
            "http://172.31.255.255/status"
    })
    @DisplayName("Should reject RFC 1918 private IPv4 addresses")
    void shouldRejectRfc1918PrivateIps(String url) {
        assertThatThrownBy(() -> validator.validate(url))
                .isInstanceOf(SsrfSecurityException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://api.github.com/v1",
            "https://api.stripe.com/v1/charges",
            "http://api.weatherapi.com/v1/current.json",
            "https://httpbin.org/get"
    })
    @DisplayName("Should accept valid public HTTP/HTTPS URLs")
    void shouldAcceptValidPublicUrls(String url) {
        assertThatCode(() -> validator.validate(url))
                .doesNotThrowAnyException();
    }
}
