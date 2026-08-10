package com.marketplace.inventory.reservation.messaging;

import java.time.Instant;
import java.util.UUID;

public record InventorySettlementResult(
    UUID eventId,
    UUID orderId,
    UUID reservationId,
    String outcome,
    String reason,
    Instant occurredAt
) {
}
