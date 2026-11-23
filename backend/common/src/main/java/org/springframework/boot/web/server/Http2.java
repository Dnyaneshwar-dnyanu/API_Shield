package org.springframework.boot.web.server;

/**
 * Backward compatibility bridge for Spring Cloud Gateway expecting
 * Http2 at org.springframework.boot.web.server.Http2.
 */
public class Http2 {

    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
