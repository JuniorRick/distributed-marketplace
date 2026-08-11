package com.marketplace.payments.payment.messaging;

import java.time.Instant;
import java.util.UUID;

public record RefundPaymentCommand(
        UUID eventId,
        UUID orderId,
        String reason,
        Instant occurredAt
) {
}
