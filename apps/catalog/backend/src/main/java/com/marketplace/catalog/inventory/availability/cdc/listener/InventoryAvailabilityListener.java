package com.marketplace.catalog.inventory.availability.cdc.listener;

import static com.marketplace.catalog.inventory.availability.cdc.InventoryAvailabilityKafkaConfiguration.INVENTORY_AVAILABILITY_TOPIC;

import com.marketplace.catalog.inventory.availability.cdc.projection.InventoryAvailabilityProjection;
import com.marketplace.catalog.inventory.availability.repository.InventoryAvailability;
import com.marketplace.catalog.inventory.availability.repository.InventoryAvailabilityRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class InventoryAvailabilityListener {

    private final ObjectMapper objectMapper;
    private final InventoryAvailabilityRepository inventoryAvailabilityRepository;

    public InventoryAvailabilityListener(ObjectMapper objectMapper, InventoryAvailabilityRepository inventoryAvailabilityRepository) {
        this.objectMapper = objectMapper;
        this.inventoryAvailabilityRepository = inventoryAvailabilityRepository;
    }

    @KafkaListener(topics = INVENTORY_AVAILABILITY_TOPIC)
    public void listen(String item) {
        var projection = objectMapper.readValue(item, InventoryAvailabilityProjection.class);

        InventoryAvailability inventoryAvailability = new InventoryAvailability();
        inventoryAvailability.setProductId(projection.productId());
        inventoryAvailability.setQuantity(projection.quantity());

        inventoryAvailabilityRepository.save(inventoryAvailability);
    }
}
