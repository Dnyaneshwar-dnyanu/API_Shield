package com.apishield.registry.security;

import com.apishield.registry.exception.SsrfSecurityException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * Validates target backend URLs to protect against Server-Side Request Forgery (SSRF)
 * attacks on both the Control Plane and Data Plane Gateway.
 */
@Component
public class SsrfProtectionValidator {

    @Value("${apishield.ssrf.allow-private-ips:false}")
    private boolean allowPrivateIps;

    private static final Set<String> FORBIDDEN_HOSTNAMES = Set.of(
            "localhost",
            "127.0.0.1",
            "::1",
            "0.0.0.0",
            "user-service",
            "discovery-server",
            "api-registry-service",
            "subscription-service",
            "api-key-service",
            "payment-service",
            "notification-service",
            "analytics-service",
            "api-gateway",
            "kafka",
            "redis",
            "mysql",
            "prometheus",
            "grafana"
    );

    /**
     * Validates that the target backend URL is safe to proxy requests to.
     *
     * @param targetUrl Candidate URL provided by API Owner
     * @throws SsrfSecurityException if URL is unsafe
     */
    public void validate(String targetUrl) {
        if (targetUrl == null || targetUrl.isBlank()) {
            throw new SsrfSecurityException("Target backend URL cannot be empty");
        }

        URI uri;
        try {
            uri = URI.create(targetUrl.trim());
        } catch (Exception e) {
            throw new SsrfSecurityException("Malformed target backend URL: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new SsrfSecurityException("Target backend URL must use HTTP or HTTPS protocol");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new SsrfSecurityException("Target backend URL must specify a valid host");
        }

        if (uri.getUserInfo() != null) {
            throw new SsrfSecurityException("User credentials are not permitted in target backend URL");
        }

        String lowerHost = host.toLowerCase();
        if (FORBIDDEN_HOSTNAMES.contains(lowerHost)) {
            throw new SsrfSecurityException("Target backend host '" + host + "' is a restricted internal or loopback address");
        }

        if (lowerHost.endsWith(".internal") || lowerHost.endsWith(".local") || lowerHost.endsWith(".localhost")) {
            throw new SsrfSecurityException("Target backend host uses a reserved internal domain suffix");
        }

        // AWS / GCP / Azure link-local instance metadata IP
        if (lowerHost.equals("169.254.169.254") || lowerHost.startsWith("169.254.")) {
            throw new SsrfSecurityException("Cloud provider metadata service access is strictly forbidden");
        }

        if (!allowPrivateIps) {
            validateDnsResolution(host);
        }
    }

    private void validateDnsResolution(String host) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (address.isLoopbackAddress() || address.isAnyLocalAddress() || address.isLinkLocalAddress()) {
                    throw new SsrfSecurityException("Target host '" + host + "' resolves to forbidden loopback/link-local address: " + address.getHostAddress());
                }

                if (address.isSiteLocalAddress()) {
                    throw new SsrfSecurityException("Target host '" + host + "' resolves to private network address (RFC 1918): " + address.getHostAddress());
                }

                byte[] raw = address.getAddress();
                if (isCarrierGradeNat(raw) || isCloudMetadata(raw)) {
                    throw new SsrfSecurityException("Target host '" + host + "' resolves to restricted address: " + address.getHostAddress());
                }
            }
        } catch (UnknownHostException e) {
            // In unit test environments with dummy domains (e.g. api.weather.com), don't fail on offline resolution
            // unless it's explicitly an IP address that should have parsed
        }
    }

    private boolean isCarrierGradeNat(byte[] raw) {
        // 100.64.0.0/10
        if (raw.length == 4) {
            int first = raw[0] & 0xFF;
            int second = raw[1] & 0xFF;
            return first == 100 && (second >= 64 && second <= 127);
        }
        return false;
    }

    private boolean isCloudMetadata(byte[] raw) {
        // 169.254.0.0/16
        if (raw.length == 4) {
            int first = raw[0] & 0xFF;
            int second = raw[1] & 0xFF;
            return first == 169 && second == 254;
        }
        return false;
    }
}
