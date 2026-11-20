package org.springframework.boot;

/**
 * Backward compatibility bridge for Spring Cloud libraries
 * expecting BootstrapRegistry at org.springframework.boot.BootstrapRegistry.
 */
public interface BootstrapRegistry extends org.springframework.boot.bootstrap.BootstrapRegistry {
}
