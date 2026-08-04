package com.marketplace.orders.checkout;

import java.time.Instant;
import java.util.UUID;

public record CheckoutCartCommand(
        UUID eventId,
        UUID orderId,
        UUID cartId,
        Instant occurredAt
) {
}
