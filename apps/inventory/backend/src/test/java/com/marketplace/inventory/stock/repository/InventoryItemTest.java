package com.marketplace.inventory.stock.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventoryItemTest {

    @Test
    void reservesAndReleasesAvailableInventory() {
        InventoryItem item = new InventoryItem(UUID.randomUUID(), 10);

        item.reserve(4);
        assertThat(item.getAvailableQuantity()).isEqualTo(6);
        assertThat(item.getReservedQuantity()).isEqualTo(4);

        item.release(3);
        assertThat(item.getAvailableQuantity()).isEqualTo(9);
        assertThat(item.getReservedQuantity()).isEqualTo(1);
    }

    @Test
    void rejectsReservationWhenInventoryIsInsufficient() {
        InventoryItem item = new InventoryItem(UUID.randomUUID(), 2);

        assertThatThrownBy(() -> item.reserve(3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Insufficient available inventory");
    }
}
