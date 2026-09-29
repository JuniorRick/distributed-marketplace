package com.marketplace.cart.cart.api;

import java.util.UUID;

public record CreateCartRequest(
        UUID customerId
) {
}
