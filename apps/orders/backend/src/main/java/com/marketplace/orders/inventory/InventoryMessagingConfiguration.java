package com.marketplace.orders.inventory;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InventoryMessagingConfiguration {

    public static final String EXCHANGE = "marketplace.inventory.v1";
    public static final String DEAD_LETTER_EXCHANGE = "marketplace.inventory.dlx.v1";
    public static final String RESULT_QUEUE = "orders.inventory-results.v1";
    public static final String RESULT_DEAD_LETTER_QUEUE = "orders.inventory-results.dlq.v1";
    public static final String RESERVE_INVENTORY_COMMAND_ROUTING_KEY = "inventory.command.reserve.v1";
    public static final String COMMIT_INVENTORY_COMMAND_ROUTING_KEY = "inventory.command.commit.v1";
    public static final String RELEASE_INVENTORY_COMMAND_ROUTING_KEY = "inventory.command.release.v1";
    public static final String INVENTORY_RESERVED_EVENT_ROUTING_KEY = "inventory.event.reserved.v1";
    public static final String INVENTORY_RESERVATION_REJECTED_EVENT_ROUTING_KEY = "inventory.event.reservation-rejected.v1";
    public static final String INVENTORY_COMMITTED_EVENT_ROUTING_KEY = "inventory.event.committed.v1";
    public static final String INVENTORY_RELEASED_EVENT_ROUTING_KEY = "inventory.event.released.v1";

    @Bean
    DirectExchange inventoryExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange inventoryDeadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue inventoryResultQueue() {
        return QueueBuilder.durable(RESULT_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .build();
    }

    @Bean
    Queue inventoryResultDeadLetterQueue() {
        return QueueBuilder.durable(RESULT_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding inventoryReservedBinding() {
        return BindingBuilder.bind(inventoryResultQueue())
                .to(inventoryExchange())
                .with(INVENTORY_RESERVED_EVENT_ROUTING_KEY);
    }

    @Bean
    Binding inventoryReservationRejectedBinding() {
        return BindingBuilder.bind(inventoryResultQueue())
                .to(inventoryExchange())
                .with(INVENTORY_RESERVATION_REJECTED_EVENT_ROUTING_KEY);
    }

    @Bean
    Binding inventoryCommittedBinding() {
        return BindingBuilder.bind(inventoryResultQueue())
                .to(inventoryExchange())
                .with(INVENTORY_COMMITTED_EVENT_ROUTING_KEY);
    }

    @Bean
    Binding inventoryReleasedBinding() {
        return BindingBuilder.bind(inventoryResultQueue())
                .to(inventoryExchange())
                .with(INVENTORY_RELEASED_EVENT_ROUTING_KEY);
    }

    @Bean
    Binding inventoryResultDeadLetterReservedBinding() {
        return BindingBuilder.bind(inventoryResultDeadLetterQueue())
                .to(inventoryDeadLetterExchange())
                .with(INVENTORY_RESERVED_EVENT_ROUTING_KEY);
    }

    @Bean
    Binding inventoryResultDeadLetterRejectedBinding() {
        return BindingBuilder.bind(inventoryResultDeadLetterQueue())
                .to(inventoryDeadLetterExchange())
                .with(INVENTORY_RESERVATION_REJECTED_EVENT_ROUTING_KEY);
    }

    @Bean
    Binding inventoryResultDeadLetterCommittedBinding() {
        return BindingBuilder.bind(inventoryResultDeadLetterQueue())
                .to(inventoryDeadLetterExchange())
                .with(INVENTORY_COMMITTED_EVENT_ROUTING_KEY);
    }

    @Bean
    Binding inventoryResultDeadLetterReleasedBinding() {
        return BindingBuilder.bind(inventoryResultDeadLetterQueue())
                .to(inventoryDeadLetterExchange())
                .with(INVENTORY_RELEASED_EVENT_ROUTING_KEY);
    }
}
