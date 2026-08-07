package com.marketplace.inventory.stock.api;

import com.marketplace.inventory.stock.StockService;
import com.marketplace.inventory.stock.repository.InventoryItem;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping("/{productId}/replenishments")
    public ResponseEntity<InventoryItemResponse> replenish(
            @PathVariable UUID productId,
            @Valid @RequestBody ReplenishStockRequest request
    ) {
        InventoryItem item = stockService.replenish(productId, request.quantity());
        return ResponseEntity.ok(new InventoryItemResponse(
                item.getProductId(),
                item.getAvailableQuantity(),
                item.getReservedQuantity()
        ));
    }
}
