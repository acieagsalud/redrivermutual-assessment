package com.claimportal.claims.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("claims.events")
public record EventsProperties(String exchange, String routingKey, Duration confirmTimeout) {
}
