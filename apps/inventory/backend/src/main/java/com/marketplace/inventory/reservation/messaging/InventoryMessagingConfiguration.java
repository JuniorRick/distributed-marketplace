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
    public static final String COMMIT_REQUEST_QUEUE = "inventory.commit-requests.v1";
    public static final String COMMIT_REQUEST_DEAD_LETTER_QUEUE = "inventory.commit-requests.dlq.v1";
    public static final String RELEASE_REQUEST_QUEUE = "inventory.release-requests.v1";
    public static final String RELEASE_REQUEST_DEAD_LETTER_QUEUE = "inventory.release-requests.dlq.v1";
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
    Queue inventoryCommitRequestQueue() {
        return QueueBuilder.durable(COMMIT_REQUEST_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(COMMIT_INVENTORY_COMMAND_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue inventoryCommitRequestDeadLetterQueue() {
        return QueueBuilder.durable(COMMIT_REQUEST_DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Queue inventoryReleaseRequestQueue() {
        return QueueBuilder.durable(RELEASE_REQUEST_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(RELEASE_INVENTORY_COMMAND_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue inventoryReleaseRequestDeadLetterQueue() {
        return QueueBuilder.durable(RELEASE_REQUEST_DEAD_LETTER_QUEUE).build();
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

    @Bean
    Binding inventoryCommitRequestBinding() {
        return BindingBuilder.bind(inventoryCommitRequestQueue())
                .to(inventoryExchange())
                .with(COMMIT_INVENTORY_COMMAND_ROUTING_KEY);
    }

    @Bean
    Binding inventoryCommitRequestDeadLetterBinding() {
        return BindingBuilder.bind(inventoryCommitRequestDeadLetterQueue())
                .to(inventoryDeadLetterExchange())
                .with(COMMIT_INVENTORY_COMMAND_ROUTING_KEY);
    }

    @Bean
    Binding inventoryReleaseRequestBinding() {
        return BindingBuilder.bind(inventoryReleaseRequestQueue())
                .to(inventoryExchange())
                .with(RELEASE_INVENTORY_COMMAND_ROUTING_KEY);
    }

    @Bean
    Binding inventoryReleaseRequestDeadLetterBinding() {
        return BindingBuilder.bind(inventoryReleaseRequestDeadLetterQueue())
                .to(inventoryDeadLetterExchange())
                .with(RELEASE_INVENTORY_COMMAND_ROUTING_KEY);
    }
}
