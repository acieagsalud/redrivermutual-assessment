package com.claimportal.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The consumer owns its queue. Messages that fail all retries (or cannot be parsed) are rejected
 * and RabbitMQ moves them to the dead-letter queue, where they can be inspected and replayed.
 */
@Configuration
public class RabbitTopologyConfig {

    @Bean
    Declarables claimSubmittedTopology(QueueProperties props) {
        TopicExchange events = ExchangeBuilder.topicExchange(props.exchange()).durable(true).build();
        DirectExchange deadLetters = ExchangeBuilder.directExchange(props.deadLetterExchange()).durable(true).build();

        Queue queue = QueueBuilder.durable(props.name())
                .deadLetterExchange(props.deadLetterExchange())
                .deadLetterRoutingKey(props.deadLetterQueue())
                .build();
        Queue deadLetterQueue = QueueBuilder.durable(props.deadLetterQueue()).build();

        Binding binding = BindingBuilder.bind(queue).to(events).with(props.routingKey());
        Binding deadLetterBinding = BindingBuilder.bind(deadLetterQueue).to(deadLetters).with(props.deadLetterQueue());

        return new Declarables(events, deadLetters, queue, deadLetterQueue, binding, deadLetterBinding);
    }
}
