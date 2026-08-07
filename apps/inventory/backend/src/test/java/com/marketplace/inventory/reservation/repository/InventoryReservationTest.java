package com.marketplace.inventory.reservation.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marketplace.inventory.reservation.ReservationStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventoryReservationTest {

    @Test
    void reservedInventoryCanBeCommitted() {
        InventoryReservation reservation = new InventoryReservation(UUID.randomUUID());
        reservation.addItem(UUID.randomUUID(), 2);

        reservation.markReserved();
        reservation.commit();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.COMMITTED);
    }

    @Test
    void rejectedReservationCannotBeReserved() {
        InventoryReservation reservation = new InventoryReservation(UUID.randomUUID());
        reservation.addItem(UUID.randomUUID(), 2);
        reservation.reject("Insufficient stock");

        assertThatThrownBy(reservation::markReserved)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only a pending reservation can be reserved");
    }
}
