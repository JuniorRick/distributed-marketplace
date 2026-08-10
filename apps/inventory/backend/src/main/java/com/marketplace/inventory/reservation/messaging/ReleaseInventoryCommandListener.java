package com.marketplace.inventory.reservation.messaging;

import com.marketplace.inventory.reservation.InventorySettlementService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class ReleaseInventoryCommandListener {

    private final ObjectMapper objectMapper;
    private final InventorySettlementService inventorySettlementService;

    public ReleaseInventoryCommandListener(
        ObjectMapper objectMapper,
        InventorySettlementService inventorySettlementService
    ) {
        this.objectMapper = objectMapper;
        this.inventorySettlementService = inventorySettlementService;
    }

    @RabbitListener(queues = InventoryMessagingConfiguration.RELEASE_REQUEST_QUEUE)
    public void handle(String payload) throws JacksonException {
        inventorySettlementService.handle(objectMapper.readValue(
            payload,
            ReleaseInventoryCommand.class
        ));
    }
}
