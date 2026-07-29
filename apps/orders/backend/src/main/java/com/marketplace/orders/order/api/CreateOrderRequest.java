package com.marketplace.orders.order.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull UUID cartId
) {
}
