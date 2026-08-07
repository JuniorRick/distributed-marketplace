package com.marketplace.inventory.reservation.messaging;

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
public class InventoryMessagingConfiguration {

    public static final String EXCHANGE = "marketplace.inventory.v1";
    public static final String DEAD_LETTER_EXCHANGE = "marketplace.inventory.dlx.v1";
    public static final String REQUEST_QUEUE = "inventory.reserve-requests.v1";
    public static final String REQUEST_DEAD_LETTER_QUEUE = "inventory.reserve-requests.dlq.v1";
    public static final String RESERVE_INVENTORY_COMMAND_ROUTING_KEY = "inventory.command.reserve.v1";
    public static final String INVENTORY_RESERVED_EVENT_ROUTING_KEY = "inventory.event.reserved.v1";
    public static final String INVENTORY_RESERVATION_REJECTED_EVENT_ROUTING_KEY = "inventory.event.reservation-rejected.v1";

    @Bean
    DirectExchange inventoryExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange inventoryDeadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue inventoryReservationRequestQueue() {
        return QueueBuilder.durable(REQUEST_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(RESERVE_INVENTORY_COMMAND_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue inventoryReservationRequestDeadLetterQueue() {
        return QueueBuilder.durable(REQUEST_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding inventoryReservationRequestBinding() {
        return BindingBuilder.bind(inventoryReservationRequestQueue())
                .to(inventoryExchange())
                .with(RESERVE_INVENTORY_COMMAND_ROUTING_KEY);
    }

    @Bean
    Binding inventoryReservationRequestDeadLetterBinding() {
        return BindingBuilder.bind(inventoryReservationRequestDeadLetterQueue())
                .to(inventoryDeadLetterExchange())
                .with(RESERVE_INVENTORY_COMMAND_ROUTING_KEY);
    }
}
