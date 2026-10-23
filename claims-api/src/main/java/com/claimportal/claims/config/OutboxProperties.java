package com.claimportal.claims.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** The poll interval is read directly by {@code OutboxPoller}'s {@code @Scheduled} placeholder. */
@ConfigurationProperties("claims.outbox")
public record OutboxProperties(int batchSize) {
}
