package com.marketplace.inventory.cdc.projection;

import java.util.UUID;

public record InventoryAvailabilityProjection(UUID productId, int quantity) {

}
