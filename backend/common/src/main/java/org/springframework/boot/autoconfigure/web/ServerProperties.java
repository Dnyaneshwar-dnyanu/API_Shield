package org.springframework.boot.autoconfigure.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Backward compatibility bridge for Spring Cloud libraries
 * expecting ServerProperties at org.springframework.boot.autoconfigure.web.ServerProperties.
 */
@Component
@ConfigurationProperties(prefix = "server", ignoreUnknownFields = true)
public class ServerProperties {

    private final Servlet servlet = new Servlet();

    public ServerProperties() {
    }

    public Servlet getServlet() {
        return servlet;
    }

    public static class Servlet {
        private String contextPath = "";

        public String getContextPath() {
            return contextPath;
        }

        public void setContextPath(String contextPath) {
            this.contextPath = contextPath;
        }
    }
}
