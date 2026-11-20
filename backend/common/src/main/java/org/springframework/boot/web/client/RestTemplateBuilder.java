package org.springframework.boot.web.client;

import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Backward compatibility bridge for Spring Cloud load balancer post processor
 * expecting RestTemplateBuilder at org.springframework.boot.web.client.RestTemplateBuilder.
 */
public class RestTemplateBuilder {

    private final List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();

    public RestTemplateBuilder() {
    }

    public RestTemplateBuilder interceptors(ClientHttpRequestInterceptor... interceptors) {
        if (interceptors != null) {
            this.interceptors.addAll(Arrays.asList(interceptors));
        }
        return this;
    }

    public RestTemplate build() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(this.interceptors);
        return restTemplate;
    }
}
