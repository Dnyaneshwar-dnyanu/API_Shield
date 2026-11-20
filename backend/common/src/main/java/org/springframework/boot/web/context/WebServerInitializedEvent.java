package org.springframework.boot.web.context;

import org.springframework.boot.web.server.WebServer;

/**
 * Backward compatibility bridge for Spring Boot 3 / Spring Cloud libraries
 * accessing WebServerInitializedEvent at org.springframework.boot.web.context.
 */
public abstract class WebServerInitializedEvent extends org.springframework.boot.web.server.context.WebServerInitializedEvent {

    protected WebServerInitializedEvent(WebServer webServer) {
        super(webServer);
    }
}
