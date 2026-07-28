package com.marketplace.cart.cart.api;

import com.marketplace.cart.cart.CartStatus;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        UUID customerId,
        CartStatus status,
        List<CartItemResponse> items,
        MoneyResponse subtotal
) {
}