package com.marketplace.orders.order.api;

import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        String productSku,
        String productName,
        MoneyResponse unitPrice,
        Integer quantity,
        MoneyResponse lineTotal
) {
}
