package com.marketplace.inventory.reservation.messaging;

import java.time.Instant;
import java.util.UUID;

public record ReleaseInventoryCommand(
    UUID eventId,
    UUID orderId,
    Instant occurredAt
) {
}
