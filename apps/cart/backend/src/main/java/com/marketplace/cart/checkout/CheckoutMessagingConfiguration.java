package com.marketplace.cart.checkout;

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
    public static final String REQUEST_QUEUE = "cart.checkout-requests.v1";
    public static final String REQUEST_DEAD_LETTER_QUEUE = "cart.checkout-requests.dlq.v1";
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
    Queue checkoutRequestQueue() {
        return QueueBuilder.durable(REQUEST_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(CHECKOUT_CART_COMMAND_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue checkoutRequestDeadLetterQueue() {
        return QueueBuilder.durable(REQUEST_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding checkoutRequestBinding(Queue checkoutRequestQueue, DirectExchange checkoutExchange) {
        return BindingBuilder.bind(checkoutRequestQueue)
                .to(checkoutExchange)
                .with(CHECKOUT_CART_COMMAND_ROUTING_KEY);
    }

    @Bean
    Binding checkoutRequestDeadLetterBinding(
            Queue checkoutRequestDeadLetterQueue,
            DirectExchange checkoutDeadLetterExchange
    ) {
        return BindingBuilder.bind(checkoutRequestDeadLetterQueue)
                .to(checkoutDeadLetterExchange)
                .with(CHECKOUT_CART_COMMAND_ROUTING_KEY);
    }
}
