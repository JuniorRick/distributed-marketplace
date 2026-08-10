package com.marketplace.payments.payment.api;

import com.marketplace.payments.payment.PaymentStatus;
import com.marketplace.payments.payment.repository.Payment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String gatewayReference,
        String failureReason,
        Instant processedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getPublicId(),
                payment.getOrderId(),
                payment.getCustomerId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getGatewayReference(),
                payment.getFailureReason(),
                payment.getProcessedAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
