package com.marketplace.orders.order.api;

import com.marketplace.orders.order.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID sourceCartId,
        UUID customerId,
        OrderStatus status,
        String failureReason,
        List<OrderItemResponse> items,
        MoneyResponse total,
        Instant createdAt
) {
}
