package com.marketplace.orders.inventory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReserveInventoryCommand(
        UUID eventId,
        UUID orderId,
        List<Item> items,
        Instant occurredAt
) {
    public record Item(UUID productId, int quantity) {
    }
}
