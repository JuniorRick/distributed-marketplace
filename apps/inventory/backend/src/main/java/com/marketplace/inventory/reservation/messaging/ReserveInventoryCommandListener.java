package com.marketplace.inventory.reservation.messaging;

import com.marketplace.inventory.reservation.InventoryReservationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class ReserveInventoryCommandListener {

    private final ObjectMapper objectMapper;
    private final InventoryReservationService inventoryReservationService;

    public ReserveInventoryCommandListener(
            ObjectMapper objectMapper,
            InventoryReservationService inventoryReservationService
    ) {
        this.objectMapper = objectMapper;
        this.inventoryReservationService = inventoryReservationService;
    }

    @RabbitListener(queues = InventoryMessagingConfiguration.REQUEST_QUEUE)
    public void handle(String payload) throws JacksonException {
        ReserveInventoryCommand command = objectMapper.readValue(payload, ReserveInventoryCommand.class);
        inventoryReservationService.handle(command);
    }
}
