package com.marketplace.catalog.inventory.availability.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryAvailabilityRepository extends JpaRepository<InventoryAvailability, UUID> {

}
