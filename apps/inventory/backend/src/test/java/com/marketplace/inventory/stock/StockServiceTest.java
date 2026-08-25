package com.marketplace.inventory.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketplace.inventory.cdc.outbox.InventoryAvailabilityOutboxService;
import com.marketplace.inventory.stock.repository.InventoryItem;
import com.marketplace.inventory.stock.repository.InventoryItemRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private InventoryAvailabilityOutboxService availabilityOutboxService;

    @InjectMocks
    private StockService stockService;

    @Test
    void createsStockAndReplenishesIt() {
        UUID productId = UUID.randomUUID();
        when(inventoryItemRepository.findByProductId(productId)).thenReturn(Optional.empty());
        when(inventoryItemRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InventoryItem result = stockService.replenish(productId, 20);

        assertThat(result.getProductId()).isEqualTo(productId);
        assertThat(result.getAvailableQuantity()).isEqualTo(20);
        assertThat(result.getReservedQuantity()).isZero();
        verify(availabilityOutboxService).enqueue(result);
    }
}
