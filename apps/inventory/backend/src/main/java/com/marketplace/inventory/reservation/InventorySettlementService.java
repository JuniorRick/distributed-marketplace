package com.marketplace.inventory.reservation;

import com.marketplace.inventory.messaging.OutboxService;
import com.marketplace.inventory.reservation.messaging.CommitInventoryCommand;
import com.marketplace.inventory.reservation.messaging.InventoryMessagingConfiguration;
import com.marketplace.inventory.reservation.messaging.InventorySettlementResult;
import com.marketplace.inventory.reservation.messaging.ReleaseInventoryCommand;
import com.marketplace.inventory.reservation.repository.InventoryReservation;
import com.marketplace.inventory.reservation.repository.InventoryReservationRepository;
import com.marketplace.inventory.stock.repository.InventoryItem;
import com.marketplace.inventory.stock.repository.InventoryItemRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventorySettlementService {

    private static final String COMMIT_COMMAND_EVENT_TYPE = "CommitInventoryCommand.v1";
    private static final String RELEASE_COMMAND_EVENT_TYPE = "ReleaseInventoryCommand.v1";
    private static final String COMMITTED_EVENT_TYPE = "InventoryCommittedEvent.v1";
    private static final String RELEASED_EVENT_TYPE = "InventoryReleasedEvent.v1";

    private final JdbcTemplate jdbcTemplate;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryReservationRepository reservationRepository;
    private final OutboxService outboxService;

    public InventorySettlementService(
        JdbcTemplate jdbcTemplate,
        InventoryItemRepository inventoryItemRepository,
        InventoryReservationRepository reservationRepository,
        OutboxService outboxService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.inventoryItemRepository = inventoryItemRepository;
        this.reservationRepository = reservationRepository;
        this.outboxService = outboxService;
    }

    @Transactional
    public void handle(CommitInventoryCommand command) {
        settle(
            command.eventId(),
            command.orderId(),
            COMMIT_COMMAND_EVENT_TYPE,
            ReservationStatus.COMMITTED
        );
    }

    @Transactional
    public void handle(ReleaseInventoryCommand command) {
        settle(
            command.eventId(),
            command.orderId(),
            RELEASE_COMMAND_EVENT_TYPE,
            ReservationStatus.RELEASED
        );
    }

    private void settle(
        UUID eventId,
        UUID orderId,
        String commandEventType,
        ReservationStatus targetStatus
    ) {
        if (!claim(eventId, commandEventType)) {
            return;
        }

        InventoryReservation reservation = reservationRepository.findByOrderId(orderId)
            .orElseThrow(() -> new IllegalStateException(
                "Inventory reservation not found. Order ID=" + orderId
            ));
        if (reservation.getStatus() == targetStatus) {
            publishResult(reservation);
            return;
        }
        if (reservation.getStatus() == ReservationStatus.COMMITTED
                || reservation.getStatus() == ReservationStatus.RELEASED) {
            publishResult(reservation);
            return;
        }
        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new IllegalStateException(
                "Only reserved inventory can be " + targetStatus.name().toLowerCase()
            );
        }

        var productIds = reservation.getItems().stream()
            .map(item -> item.getProductId())
            .collect(Collectors.toSet());
        Map<UUID, InventoryItem> stockByProduct = inventoryItemRepository
            .findAllByProductIdIn(productIds)
            .stream()
            .collect(Collectors.toMap(InventoryItem::getProductId, Function.identity()));

        reservation.getItems().forEach(item -> {
            InventoryItem stock = stockByProduct.get(item.getProductId());
            if (stock == null) {
                throw new IllegalStateException(
                    "Inventory balance not found. Product ID=" + item.getProductId()
                );
            }
            if (stock.getReservedQuantity() < item.getQuantity()) {
                throw new IllegalStateException(
                    "Reserved inventory is inconsistent. Product ID=" + item.getProductId()
                );
            }
        });

        reservation.getItems().forEach(item -> {
            InventoryItem stock = stockByProduct.get(item.getProductId());
            if (targetStatus == ReservationStatus.COMMITTED) {
                stock.commit(item.getQuantity());
            } else {
                stock.release(item.getQuantity());
            }
        });
        if (targetStatus == ReservationStatus.COMMITTED) {
            reservation.commit();
        } else {
            reservation.release();
        }

        reservationRepository.saveAndFlush(reservation);
        publishResult(reservation);
    }

    private boolean claim(UUID eventId, String eventType) {
        return jdbcTemplate.update(
            """
                INSERT INTO inbox_messages(event_id, event_type, processed_at)
                VALUES (?, ?, now())
                ON CONFLICT (event_id) DO NOTHING
                """, eventId, eventType
        ) == 1;
    }

    private void publishResult(InventoryReservation reservation) {
        boolean committed = reservation.getStatus() == ReservationStatus.COMMITTED;
        InventorySettlementResult result = new InventorySettlementResult(
            UUID.randomUUID(),
            reservation.getOrderId(),
            reservation.getPublicId(),
            committed ? "COMMITTED" : "RELEASED",
            null,
            Instant.now()
        );
        outboxService.enqueue(
            result.eventId(),
            committed ? COMMITTED_EVENT_TYPE : RELEASED_EVENT_TYPE,
            committed
                ? InventoryMessagingConfiguration.INVENTORY_COMMITTED_EVENT_ROUTING_KEY
                : InventoryMessagingConfiguration.INVENTORY_RELEASED_EVENT_ROUTING_KEY,
            result
        );
    }
}
