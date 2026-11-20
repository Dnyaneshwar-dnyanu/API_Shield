package org.springframework.boot.web.servlet.context;

import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;

/**
 * Backward compatibility bridge for Spring Boot 3 / Spring Cloud libraries
 * accessing ServletWebServerInitializedEvent at org.springframework.boot.web.servlet.context.
 */
public class ServletWebServerInitializedEvent extends org.springframework.boot.web.server.servlet.context.ServletWebServerInitializedEvent {

    public ServletWebServerInitializedEvent(WebServer webServer, ServletWebServerApplicationContext applicationContext) {
        super(webServer, applicationContext);
    }
}
