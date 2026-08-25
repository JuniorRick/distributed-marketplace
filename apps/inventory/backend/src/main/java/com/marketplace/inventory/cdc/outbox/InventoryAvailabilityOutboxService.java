package com.marketplace.inventory.cdc.outbox;

import com.marketplace.inventory.cdc.projection.InventoryAvailabilityProjection;
import com.marketplace.inventory.stock.repository.InventoryItem;
import java.util.Collection;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class InventoryAvailabilityOutboxService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public InventoryAvailabilityOutboxService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void enqueueAll(Collection<InventoryItem> items) {
        items.forEach(this::enqueue);
    }

    public void enqueue(InventoryItem item) {
        var event = new InventoryAvailabilityProjection(
                item.getProductId(),
                item.getAvailableQuantity()
        );
        try {
            jdbcTemplate.update("""
                    INSERT INTO inventory_availability_outbox(
                        event_id, product_id, payload, occurred_at
                    ) VALUES (?, ?, ?, clock_timestamp())
                    """,
                    UUID.randomUUID(),
                    item.getProductId(),
                    objectMapper.writeValueAsString(event)
            );
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Inventory availability could not be serialized", exception);
        }
    }
}
