package org.springframework.boot;

/**
 * Backward compatibility bridge for Spring Cloud libraries
 * expecting ConfigurableBootstrapContext at org.springframework.boot.ConfigurableBootstrapContext.
 */
public interface ConfigurableBootstrapContext extends org.springframework.boot.bootstrap.ConfigurableBootstrapContext, BootstrapRegistry, BootstrapContext {
}
