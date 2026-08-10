package com.marketplace.inventory.reservation.messaging;

import java.time.Instant;
import java.util.UUID;

public record CommitInventoryCommand(
    UUID eventId,
    UUID orderId,
    Instant occurredAt
) {
}
