package com.marketplace.payments.payment.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResult(
        UUID eventId,
        UUID paymentId,
        UUID orderId,
        String outcome,
        BigDecimal amount,
        String currency,
        String reason,
        Instant occurredAt
) {
}
