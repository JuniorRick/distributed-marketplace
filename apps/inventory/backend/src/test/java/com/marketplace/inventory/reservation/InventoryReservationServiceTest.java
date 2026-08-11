package com.marketplace.inventory.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketplace.inventory.messaging.OutboxService;
import com.marketplace.inventory.reservation.messaging.InventoryMessagingConfiguration;
import com.marketplace.inventory.reservation.messaging.ReserveInventoryCommand;
import com.marketplace.inventory.reservation.repository.InventoryReservation;
import com.marketplace.inventory.reservation.repository.InventoryReservationRepository;
import com.marketplace.inventory.stock.repository.InventoryItem;
import com.marketplace.inventory.stock.repository.InventoryItemRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class InventoryReservationServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private InventoryReservationRepository reservationRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private InventoryReservationService service;

    @Test
    void reservesEveryRequestedItemAndPublishesReservedEvent() {
        UUID productId = UUID.randomUUID();
        ReserveInventoryCommand command = command(productId, 3);
        InventoryItem stock = new InventoryItem(productId, 10);
        prepareNewCommand(command, List.of(stock));

        service.handle(command);

        assertThat(stock.getAvailableQuantity()).isEqualTo(7);
        assertThat(stock.getReservedQuantity()).isEqualTo(3);
        ArgumentCaptor<InventoryReservation> reservation = ArgumentCaptor.forClass(
            InventoryReservation.class
        );
        verify(reservationRepository).saveAndFlush(reservation.capture());
        assertThat(reservation.getValue().getStatus()).isEqualTo(ReservationStatus.RESERVED);
        verify(outboxService).enqueue(
            any(UUID.class),
            eq("InventoryReservedEvent.v1"),
            eq(InventoryMessagingConfiguration.INVENTORY_RESERVED_EVENT_ROUTING_KEY),
            any()
        );
    }

    @Test
    void rejectsWholeReservationWhenStockIsInsufficient() {
        UUID productId = UUID.randomUUID();
        ReserveInventoryCommand command = command(productId, 3);
        InventoryItem stock = new InventoryItem(productId, 2);
        prepareNewCommand(command, List.of(stock));

        service.handle(command);

        assertThat(stock.getAvailableQuantity()).isEqualTo(2);
        assertThat(stock.getReservedQuantity()).isZero();
        ArgumentCaptor<InventoryReservation> reservation = ArgumentCaptor.forClass(
            InventoryReservation.class
        );
        verify(reservationRepository).saveAndFlush(reservation.capture());
        assertThat(reservation.getValue().getStatus()).isEqualTo(ReservationStatus.REJECTED);
        assertThat(reservation.getValue().getFailureReason()).contains("Insufficient inventory");
        verify(outboxService).enqueue(
            any(UUID.class),
            eq("InventoryReservationRejectedEvent.v1"),
            eq(InventoryMessagingConfiguration.INVENTORY_RESERVATION_REJECTED_EVENT_ROUTING_KEY),
            any()
        );
    }

    @Test
    void doesNotPartiallyReserveWhenOneOfSeveralProductsIsUnavailable() {
        UUID availableProductId = UUID.randomUUID();
        UUID unavailableProductId = UUID.randomUUID();
        ReserveInventoryCommand command = new ReserveInventoryCommand(
            UUID.randomUUID(),
            UUID.randomUUID(),
            List.of(
                new ReserveInventoryCommand.Item(availableProductId, 2),
                new ReserveInventoryCommand.Item(unavailableProductId, 2)
            ),
            Instant.now()
        );
        InventoryItem availableStock = new InventoryItem(availableProductId, 10);
        InventoryItem unavailableStock = new InventoryItem(unavailableProductId, 1);
        prepareNewCommand(command, List.of(availableStock, unavailableStock));

        service.handle(command);

        assertThat(availableStock.getAvailableQuantity()).isEqualTo(10);
        assertThat(availableStock.getReservedQuantity()).isZero();
        assertThat(unavailableStock.getAvailableQuantity()).isEqualTo(1);
        assertThat(unavailableStock.getReservedQuantity()).isZero();
    }

    private void prepareNewCommand(
        ReserveInventoryCommand command,
        List<InventoryItem> inventoryItems
    ) {
        when(jdbcTemplate.update(anyString(), eq(command.eventId()), eq("ReserveInventoryCommand.v1"))).thenReturn(1);
        when(reservationRepository.findByOrderId(command.orderId())).thenReturn(java.util.Optional.empty());
        when(inventoryItemRepository.findAllByProductIdIn(anyCollection())).thenReturn(inventoryItems);
        when(reservationRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
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
