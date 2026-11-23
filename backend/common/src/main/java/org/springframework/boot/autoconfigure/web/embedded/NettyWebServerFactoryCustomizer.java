package org.springframework.boot.autoconfigure.web.embedded;

import org.springframework.core.Ordered;

/**
 * Backward compatibility bridge for Spring Cloud Gateway expecting
 * NettyWebServerFactoryCustomizer at org.springframework.boot.autoconfigure.web.embedded.NettyWebServerFactoryCustomizer.
 */
public class NettyWebServerFactoryCustomizer implements Ordered {

    public NettyWebServerFactoryCustomizer() {
    }

    public NettyWebServerFactoryCustomizer(Object environment, Object serverProperties) {
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
