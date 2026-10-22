package com.claimportal.claims.config;

import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The publisher declares only the exchange it publishes to. Queues belong to their consumers,
 * so notification-service declares its own queue, binding and dead-letter queue.
 */
@Configuration
public class RabbitConfig {

    @Bean
    TopicExchange claimsEventsExchange(EventsProperties properties) {
        return ExchangeBuilder.topicExchange(properties.exchange()).durable(true).build();
    }
}
