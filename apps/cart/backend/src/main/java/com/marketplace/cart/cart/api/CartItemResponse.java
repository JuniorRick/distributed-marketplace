package com.marketplace.cart.cart.api;

import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID productId,
        String productSku,
        String productName,
        MoneyResponse unitPrice,
        Integer quantity,
        MoneyResponse lineTotal
) {
}