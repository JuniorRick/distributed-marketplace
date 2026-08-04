package com.marketplace.orders.checkout;

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
public class CheckoutMessagingConfiguration {

    public static final String EXCHANGE = "marketplace.checkout.v1";
    public static final String DEAD_LETTER_EXCHANGE = "marketplace.checkout.dlx.v1";
    public static final String RESULT_QUEUE = "orders.checkout-results.v1";
    public static final String RESULT_DEAD_LETTER_QUEUE = "orders.checkout-results.dlq.v1";
    public static final String CHECKOUT_CART_COMMAND_ROUTING_KEY = "cart.command.checkout.v1";
    public static final String CART_CHECKED_OUT_EVENT_ROUTING_KEY = "cart.event.checked-out.v1";
    public static final String CART_CHECKOUT_REJECTED_EVENT_ROUTING_KEY = "cart.event.checkout-rejected.v1";

    @Bean
    DirectExchange checkoutExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange checkoutDeadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue checkoutResultQueue() {
        return QueueBuilder.durable(RESULT_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .build();
    }

    @Bean
    Queue checkoutResultDeadLetterQueue() {
        return QueueBuilder.durable(RESULT_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding checkoutCompletedBinding(Queue checkoutResultQueue, DirectExchange checkoutExchange) {
        return BindingBuilder.bind(checkoutResultQueue)
                .to(checkoutExchange)
                .with(CART_CHECKED_OUT_EVENT_ROUTING_KEY);
    }

    @Bean
    Binding checkoutRejectedBinding(Queue checkoutResultQueue, DirectExchange checkoutExchange) {
        return BindingBuilder.bind(checkoutResultQueue)
                .to(checkoutExchange)
                .with(CART_CHECKOUT_REJECTED_EVENT_ROUTING_KEY);
    }

    @Bean
    Binding checkoutResultDeadLetterCompletedBinding(
            Queue checkoutResultDeadLetterQueue,
            DirectExchange checkoutDeadLetterExchange
    ) {
        return BindingBuilder.bind(checkoutResultDeadLetterQueue)
                .to(checkoutDeadLetterExchange)
                .with(CART_CHECKED_OUT_EVENT_ROUTING_KEY);
    }

    @Bean
    Binding checkoutResultDeadLetterRejectedBinding(
            Queue checkoutResultDeadLetterQueue,
            DirectExchange checkoutDeadLetterExchange
    ) {
        return BindingBuilder.bind(checkoutResultDeadLetterQueue)
                .to(checkoutDeadLetterExchange)
                .with(CART_CHECKOUT_REJECTED_EVENT_ROUTING_KEY);
    }
}
