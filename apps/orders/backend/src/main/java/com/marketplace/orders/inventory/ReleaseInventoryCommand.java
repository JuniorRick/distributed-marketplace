package com.marketplace.orders.inventory;

import java.time.Instant;
import java.util.UUID;

public record ReleaseInventoryCommand(UUID eventId, UUID orderId, Instant occurredAt) {
}
