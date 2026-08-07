package com.marketplace.inventory.stock.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    Optional<InventoryItem> findByProductId(UUID productId);

    List<InventoryItem> findAllByProductIdIn(Collection<UUID> productIds);
}
