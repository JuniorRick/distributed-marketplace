package com.marketplace.inventory.reservation.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<InventoryReservation> findByPublicId(UUID publicId);

    @EntityGraph(attributePaths = "items")
    Optional<InventoryReservation> findByOrderId(UUID orderId);
}
