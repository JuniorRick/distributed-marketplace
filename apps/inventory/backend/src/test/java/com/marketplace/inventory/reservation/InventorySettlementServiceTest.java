package com.marketplace.inventory.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.marketplace.inventory.cdc.outbox.InventoryAvailabilityOutboxService;
import com.marketplace.inventory.outbox.OutboxService;
import com.marketplace.inventory.reservation.messaging.CommitInventoryCommand;
import com.marketplace.inventory.reservation.messaging.InventoryMessagingConfiguration;
import com.marketplace.inventory.reservation.messaging.ReleaseInventoryCommand;
import com.marketplace.inventory.reservation.repository.InventoryReservation;
import com.marketplace.inventory.reservation.repository.InventoryReservationRepository;
import com.marketplace.inventory.stock.repository.InventoryItem;
import com.marketplace.inventory.stock.repository.InventoryItemRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class InventorySettlementServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private InventoryReservationRepository reservationRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private InventoryAvailabilityOutboxService availabilityOutboxService;

    @Test
    void commitsReservedStockAndPublishesCommittedEvent() {
        Fixture fixture = fixture();
        InventorySettlementService service = service();
        UUID eventId = UUID.randomUUID();
        prepare(eventId, "CommitInventoryCommand.v1", fixture);

        service.handle(new CommitInventoryCommand(eventId, fixture.orderId(), Instant.now()));

        assertThat(fixture.reservation().getStatus()).isEqualTo(ReservationStatus.COMMITTED);
        assertThat(fixture.stock().getAvailableQuantity()).isEqualTo(7);
        assertThat(fixture.stock().getReservedQuantity()).isZero();
        verifyNoInteractions(availabilityOutboxService);
        verify(outboxService).enqueue(
            any(UUID.class),
            eq("InventoryCommittedEvent.v1"),
            eq(InventoryMessagingConfiguration.INVENTORY_COMMITTED_EVENT_ROUTING_KEY),
            any()
        );
    }

    @Test
    void releasesReservedStockAndPublishesReleasedEvent() {
        Fixture fixture = fixture();
        InventorySettlementService service = service();
        UUID eventId = UUID.randomUUID();
        prepare(eventId, "ReleaseInventoryCommand.v1", fixture);

        service.handle(new ReleaseInventoryCommand(eventId, fixture.orderId(), Instant.now()));

        assertThat(fixture.reservation().getStatus()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(fixture.stock().getAvailableQuantity()).isEqualTo(10);
        assertThat(fixture.stock().getReservedQuantity()).isZero();
        verify(availabilityOutboxService).enqueueAll(argThat(
                items -> items.size() == 1 && items.contains(fixture.stock())
        ));
        verify(outboxService).enqueue(
            any(UUID.class),
            eq("InventoryReleasedEvent.v1"),
            eq(InventoryMessagingConfiguration.INVENTORY_RELEASED_EVENT_ROUTING_KEY),
            any()
        );
    }

    private InventorySettlementService service() {
        return new InventorySettlementService(
            jdbcTemplate,
            inventoryItemRepository,
            reservationRepository,
            outboxService,
            availabilityOutboxService
        );
    }

    private void prepare(UUID eventId, String eventType, Fixture fixture) {
        when(jdbcTemplate.update(anyString(), eq(eventId), eq(eventType))).thenReturn(1);
        when(reservationRepository.findByOrderId(fixture.orderId()))
            .thenReturn(Optional.of(fixture.reservation()));
        when(inventoryItemRepository.findAllByProductIdIn(anyCollection()))
            .thenReturn(List.of(fixture.stock()));
        when(reservationRepository.saveAndFlush(any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static Fixture fixture() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        InventoryReservation reservation = new InventoryReservation(orderId);
        reservation.addItem(productId, 3);
        reservation.markReserved();
        InventoryItem stock = new InventoryItem(productId, 10);
        stock.reserve(3);
        return new Fixture(orderId, reservation, stock);
    }

    private record Fixture(
        UUID orderId,
        InventoryReservation reservation,
        InventoryItem stock
    ) {
    }
}
