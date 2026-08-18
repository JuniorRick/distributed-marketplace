package com.marketplace.catalog.inventory.availability.cdc.projection;

import java.util.UUID;

public record InventoryAvailabilityProjection(UUID productId, int quantity) {

}
