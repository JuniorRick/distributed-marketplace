package com.marketplace.inventory.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketplace.inventory.messaging.OutboxPublisher;
import com.marketplace.inventory.messaging.OutboxService;
import com.marketplace.inventory.reservation.messaging.ReserveInventoryCommand;
import com.marketplace.inventory.reservation.repository.InventoryReservationRepository;
import com.marketplace.inventory.shared.InventoryIntegrationTest;
import com.marketplace.inventory.stock.repository.InventoryItem;
import com.marketplace.inventory.stock.repository.InventoryItemRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class InventoryReservationConcurrencyIT extends InventoryIntegrationTest {

    @Autowired
    private InventoryReservationService service;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @MockitoBean
    private OutboxService outboxService;

    @MockitoBean
    private OutboxPublisher outboxPublisher;

    @Test
    void concurrentReservationsDoNotOversellStock() throws Exception {
        UUID productId = UUID.randomUUID();
        inventoryItemRepository.saveAndFlush(new InventoryItem(productId, 5));

        ReserveInventoryCommand first = command(productId, 3);
        ReserveInventoryCommand second = command(productId, 3);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> firstResult = executor.submit(() -> handleWithListenerRetry(first, ready, start));
            Future<?> secondResult = executor.submit(() -> handleWithListenerRetry(second, ready, start));

            ready.await();
            start.countDown();
            firstResult.get();
            secondResult.get();
        }

        InventoryItem stock = inventoryItemRepository.findByProductId(productId).orElseThrow();
        assertThat(stock.getAvailableQuantity()).isEqualTo(2);
        assertThat(stock.getReservedQuantity()).isEqualTo(3);
        assertThat(List.of(
            reservationRepository.findByOrderId(first.orderId()).orElseThrow().getStatus(),
            reservationRepository.findByOrderId(second.orderId()).orElseThrow().getStatus()
        )).containsExactlyInAnyOrder(ReservationStatus.RESERVED, ReservationStatus.REJECTED);
    }

    private void handleWithListenerRetry(
        ReserveInventoryCommand command,
        CountDownLatch ready,
        CountDownLatch start
    ) {
        ready.countDown();
        await(start);

        for (int attempt = 1; attempt <= 5; attempt++) {
            try {
                service.handle(command);
                return;
            } catch (OptimisticLockingFailureException exception) {
                if (attempt == 5) {
                    throw exception;
                }
            }
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while coordinating reservations", exception);
        }
    }

    private static ReserveInventoryCommand command(UUID productId, int quantity) {
        return new ReserveInventoryCommand(
            UUID.randomUUID(),
            UUID.randomUUID(),
            List.of(new ReserveInventoryCommand.Item(productId, quantity)),
            Instant.now()
        );
    }
}
