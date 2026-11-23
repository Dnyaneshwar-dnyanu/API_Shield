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

    private Integer port = 8080;
    private final Servlet servlet = new Servlet();
    private final org.springframework.boot.web.server.Http2 http2 = new org.springframework.boot.web.server.Http2();

    public ServerProperties() {
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Servlet getServlet() {
        return servlet;
    }

    public org.springframework.boot.web.server.Http2 getHttp2() {
        return http2;
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
