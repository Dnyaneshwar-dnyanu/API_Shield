package org.springframework.boot.web.reactive.context;

import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.reactive.context.ReactiveWebServerApplicationContext;

/**
 * Backward compatibility bridge for Spring Boot 3 / Spring Cloud libraries
 * accessing ReactiveWebServerInitializedEvent at org.springframework.boot.web.reactive.context.
 */
public class ReactiveWebServerInitializedEvent extends org.springframework.boot.web.server.reactive.context.ReactiveWebServerInitializedEvent {

    public ReactiveWebServerInitializedEvent(WebServer webServer, ReactiveWebServerApplicationContext applicationContext) {
        super(webServer, applicationContext);
    }
}
