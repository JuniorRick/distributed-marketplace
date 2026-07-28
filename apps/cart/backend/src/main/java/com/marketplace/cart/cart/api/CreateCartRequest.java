package com.marketplace.cart.cart.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateCartRequest(
        @NotNull UUID customerId
) {
}
