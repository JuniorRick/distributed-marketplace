package com.marketplace.orders.inventory;

import java.time.Instant;
import java.util.UUID;

public record CommitInventoryCommand(UUID eventId, UUID orderId, Instant occurredAt) {
}
