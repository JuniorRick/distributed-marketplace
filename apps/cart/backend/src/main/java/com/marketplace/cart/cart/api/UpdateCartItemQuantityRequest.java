package com.marketplace.cart.cart.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartItemQuantityRequest(
        @NotNull
        @Min(1) @Max(99) Integer quantity
) {
}
