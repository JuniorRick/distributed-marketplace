package com.marketplace.notifications.recipient;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomerMessagingConfiguration {
    public static final String EXCHANGE = "marketplace.customers.v1";
    public static final String DEAD_LETTER_EXCHANGE = "marketplace.customers.dlx.v1";
    public static final String QUEUE = "notifications.customer-profiles.v1";
    public static final String DEAD_LETTER_QUEUE = "notifications.customer-profiles.dlq.v1";
    public static final String CREATED = "customer.event.created.v1";
    public static final String CONTACT_UPDATED = "customer.event.contact-updated.v1";
    public static final String PREFERENCES_UPDATED = "customer.event.notification-preferences-updated.v1";

    @Bean
    DirectExchange customerExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange customerDeadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue customerProfileQueue() {
        return QueueBuilder.durable(QUEUE).deadLetterExchange(DEAD_LETTER_EXCHANGE).build();
    }

    @Bean
    Queue customerProfileDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding customerCreatedBinding(Queue customerProfileQueue, DirectExchange customerExchange) {
        return bind(customerProfileQueue, customerExchange, CREATED);
    }

    @Bean
    Binding customerContactUpdatedBinding(Queue customerProfileQueue, DirectExchange customerExchange) {
        return bind(customerProfileQueue, customerExchange, CONTACT_UPDATED);
    }

    @Bean
    Binding customerPreferencesUpdatedBinding(Queue customerProfileQueue, DirectExchange customerExchange) {
        return bind(customerProfileQueue, customerExchange, PREFERENCES_UPDATED);
    }

    @Bean
    Binding customerCreatedDeadLetterBinding(Queue customerProfileDeadLetterQueue, DirectExchange customerDeadLetterExchange) {
        return bind(customerProfileDeadLetterQueue, customerDeadLetterExchange, CREATED);
    }

    @Bean
    Binding customerContactUpdatedDeadLetterBinding(Queue customerProfileDeadLetterQueue, DirectExchange customerDeadLetterExchange) {
        return bind(customerProfileDeadLetterQueue, customerDeadLetterExchange, CONTACT_UPDATED);
    }

    @Bean
    Binding customerPreferencesUpdatedDeadLetterBinding(Queue customerProfileDeadLetterQueue, DirectExchange customerDeadLetterExchange) {
        return bind(customerProfileDeadLetterQueue, customerDeadLetterExchange, PREFERENCES_UPDATED);
    }

    private static Binding bind(Queue queue, DirectExchange exchange, String routingKey) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }
}
