package com.marketplace.notifications.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderLifecycleEvent(
    UUID eventId,
    UUID orderId,
    UUID customerId,
    String status,
    BigDecimal totalAmount,
    String currency,
    String reason,
    Instant occurredAt
) {
}
