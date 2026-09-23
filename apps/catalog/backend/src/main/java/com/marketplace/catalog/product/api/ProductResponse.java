package com.marketplace.catalog.product.api;

import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        String description,
        MoneyResponse price,
        String status,
        Integer quantity
) {
}
