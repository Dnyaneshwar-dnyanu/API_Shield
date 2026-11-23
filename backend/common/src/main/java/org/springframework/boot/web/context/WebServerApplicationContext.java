package org.springframework.boot.web.context;

import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationContext;

/**
 * Compatibility bridge interface for Spring Cloud Gateway 4.2.0 running on Spring Boot 4.
 * In Spring Boot 4, WebServerApplicationContext was relocated to org.springframework.boot.web.server.context.
 */
public interface WebServerApplicationContext extends ApplicationContext {

    WebServer getWebServer();

    String getServerNamespace();

    static boolean hasServerNamespace(ApplicationContext context, String serverNamespace) {
        return org.springframework.boot.web.server.context.WebServerApplicationContext.hasServerNamespace(context, serverNamespace);
    }

    static String getServerNamespace(ApplicationContext context) {
        return org.springframework.boot.web.server.context.WebServerApplicationContext.getServerNamespace(context);
    }
}
