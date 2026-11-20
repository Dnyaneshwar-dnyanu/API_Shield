package org.springframework.boot.web.context;

/**
 * Backward compatibility bridge for Spring Boot 3 / Spring Cloud libraries
 * accessing WebServerApplicationContext at org.springframework.boot.web.context.
 */
public interface WebServerApplicationContext extends org.springframework.boot.web.server.context.WebServerApplicationContext {
}
