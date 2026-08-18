package com.marketplace.inventory.stock;

import com.marketplace.inventory.cdc.outbox.InventoryAvailabilityOutboxService;
import com.marketplace.inventory.stock.repository.InventoryItem;
import com.marketplace.inventory.stock.repository.InventoryItemRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryAvailabilityOutboxService availabilityOutboxService;

    public StockService(
            InventoryItemRepository inventoryItemRepository,
            InventoryAvailabilityOutboxService availabilityOutboxService
    ) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.availabilityOutboxService = availabilityOutboxService;
    }

    @Transactional
    public InventoryItem replenish(UUID productId, int quantity) {
        InventoryItem item = inventoryItemRepository.findByProductId(productId)
                .orElseGet(() -> new InventoryItem(productId, 0));
        item.replenish(quantity);
        InventoryItem savedItem = inventoryItemRepository.save(item);
        availabilityOutboxService.enqueue(savedItem);
        return savedItem;
    }
}
