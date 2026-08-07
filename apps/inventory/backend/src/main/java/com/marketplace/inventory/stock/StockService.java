package com.marketplace.inventory.stock;

import com.marketplace.inventory.stock.repository.InventoryItem;
import com.marketplace.inventory.stock.repository.InventoryItemRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private final InventoryItemRepository inventoryItemRepository;

    public StockService(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Transactional
    public InventoryItem replenish(UUID productId, int quantity) {
        InventoryItem item = inventoryItemRepository.findByProductId(productId)
                .orElseGet(() -> new InventoryItem(productId, 0));
        item.replenish(quantity);
        return inventoryItemRepository.save(item);
    }
}
