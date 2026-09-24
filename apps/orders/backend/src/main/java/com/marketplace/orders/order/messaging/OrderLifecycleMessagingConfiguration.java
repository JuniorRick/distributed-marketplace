package com.marketplace.orders.order.messaging;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderLifecycleMessagingConfiguration {

    public static final String EXCHANGE = "marketplace.orders.v1";
    public static final String ORDER_CONFIRMED_EVENT_ROUTING_KEY = "order.event.confirmed.v1";
    public static final String ORDER_REJECTED_EVENT_ROUTING_KEY = "order.event.rejected.v1";
    public static final String ORDER_REFUNDED_EVENT_ROUTING_KEY = "order.event.refunded.v1";

    @Bean
    DirectExchange orderLifecycleExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }
}
