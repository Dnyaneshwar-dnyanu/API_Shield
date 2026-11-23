package com.apishield.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "apishield.notification")
@Getter
@Setter
public class NotificationProperties {

    private String fromEmail = "no-reply@apishield.io";
    private String senderName = "APIShield Platform";
    private boolean mockDelivery = true;
}
