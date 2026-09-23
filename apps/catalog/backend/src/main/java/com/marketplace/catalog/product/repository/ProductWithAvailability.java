package com.marketplace.catalog.product.repository;

import com.marketplace.catalog.product.ProductStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductWithAvailability(
    UUID id,
    String sku,
    String name,
    String description,
    BigDecimal priceAmount,
    String currency,
    ProductStatus status,
    int availableQuantity
) {

}