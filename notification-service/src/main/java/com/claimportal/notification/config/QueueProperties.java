package com.claimportal.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("notification.queue")
public record QueueProperties(String exchange, String routingKey, String name, String deadLetterExchange,
                              String deadLetterQueue) {
}
