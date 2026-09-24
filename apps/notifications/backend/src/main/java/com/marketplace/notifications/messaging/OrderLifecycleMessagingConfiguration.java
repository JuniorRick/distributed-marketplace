package com.marketplace.notifications.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class OrderLifecycleMessagingConfiguration {
    public static final String EXCHANGE = "marketplace.orders.v1";
    public static final String DEAD_LETTER_EXCHANGE = "marketplace.orders.dlx.v1";
    public static final String QUEUE = "notifications.order-lifecycle.v1";
    public static final String DEAD_LETTER_QUEUE = "notifications.order-lifecycle.dlq.v1";
    public static final String ORDER_CONFIRMED_EVENT_ROUTING_KEY = "order.event.confirmed.v1";
    public static final String ORDER_REJECTED_EVENT_ROUTING_KEY = "order.event.rejected.v1";
    public static final String ORDER_REFUNDED_EVENT_ROUTING_KEY = "order.event.refunded.v1";

    @Bean
    DirectExchange orderLifecycleExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange orderLifecycleDeadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue orderLifecycleQueue() {
        return QueueBuilder.durable(QUEUE).deadLetterExchange(DEAD_LETTER_EXCHANGE).build();
    }

    @Bean
    Queue orderLifecycleDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding orderConfirmedBinding(Queue orderLifecycleQueue, DirectExchange orderLifecycleExchange) {
        return BindingBuilder.bind(orderLifecycleQueue).to(orderLifecycleExchange)
            .with(ORDER_CONFIRMED_EVENT_ROUTING_KEY);
    }

    @Bean
    Binding orderRejectedBinding(Queue orderLifecycleQueue, DirectExchange orderLifecycleExchange) {
        return BindingBuilder.bind(orderLifecycleQueue).to(orderLifecycleExchange)
            .with(ORDER_REJECTED_EVENT_ROUTING_KEY);
    }

    @Bean
    Binding orderRefundedBinding(Queue orderLifecycleQueue, DirectExchange orderLifecycleExchange) {
        return BindingBuilder.bind(orderLifecycleQueue).to(orderLifecycleExchange)
            .with(ORDER_REFUNDED_EVENT_ROUTING_KEY);
    }

    @Bean
    Binding orderConfirmedDeadLetterBinding(Queue orderLifecycleDeadLetterQueue, DirectExchange orderLifecycleDeadLetterExchange) {
        return BindingBuilder.bind(orderLifecycleDeadLetterQueue).to(orderLifecycleDeadLetterExchange)
            .with(ORDER_CONFIRMED_EVENT_ROUTING_KEY);
    }

    @Bean
    Binding orderRejectedDeadLetterBinding(Queue orderLifecycleDeadLetterQueue, DirectExchange orderLifecycleDeadLetterExchange) {
        return BindingBuilder.bind(orderLifecycleDeadLetterQueue).to(orderLifecycleDeadLetterExchange)
            .with(ORDER_REJECTED_EVENT_ROUTING_KEY);
    }

    @Bean
    Binding orderRefundedDeadLetterBinding(Queue orderLifecycleDeadLetterQueue, DirectExchange orderLifecycleDeadLetterExchange) {
        return BindingBuilder.bind(orderLifecycleDeadLetterQueue).to(orderLifecycleDeadLetterExchange)
            .with(ORDER_REFUNDED_EVENT_ROUTING_KEY);
    }
}
