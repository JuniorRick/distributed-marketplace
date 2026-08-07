package com.marketplace.inventory.stock.api;

import java.util.UUID;

public record InventoryItemResponse(
        UUID productId,
        int availableQuantity,
        int reservedQuantity
) {
}
