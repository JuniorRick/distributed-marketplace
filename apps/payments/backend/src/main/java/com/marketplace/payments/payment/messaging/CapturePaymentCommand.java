package com.marketplace.payments.payment.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CapturePaymentCommand(
        UUID eventId,
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        String currency,
        Instant occurredAt
) {
}
