package com.marketplace.inventory.stock.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ReplenishStockRequest(
        @Min(1) @Max(1_000_000) int quantity
) {
}
