package org.springframework.boot;

/**
 * Backward compatibility bridge for Spring Cloud libraries
 * expecting BootstrapContext at org.springframework.boot.BootstrapContext.
 */
public interface BootstrapContext extends org.springframework.boot.bootstrap.BootstrapContext {
}
