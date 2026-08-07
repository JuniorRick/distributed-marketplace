package com.marketplace.orders.inventory;

import com.marketplace.orders.order.OrderPersistenceService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class InventoryResultListener {

    private final ObjectMapper objectMapper;
    private final OrderPersistenceService orderPersistenceService;

    public InventoryResultListener(
            ObjectMapper objectMapper,
            OrderPersistenceService orderPersistenceService
    ) {
        this.objectMapper = objectMapper;
        this.orderPersistenceService = orderPersistenceService;
    }

    @RabbitListener(queues = InventoryMessagingConfiguration.RESULT_QUEUE)
    public void handle(String payload) throws JacksonException {
        InventoryReservationResult event = objectMapper.readValue(
                payload,
                InventoryReservationResult.class
        );
        orderPersistenceService.applyInventoryResult(event);
    }

    public record InventoryReservationResult(
            UUID eventId,
            UUID orderId,
            UUID reservationId,
            String outcome,
            String reason,
            Instant occurredAt
    ) {
    }
}
