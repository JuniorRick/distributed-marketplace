package com.marketplace.inventory.reservation;

import com.marketplace.inventory.cdc.outbox.InventoryAvailabilityOutboxService;
import com.marketplace.inventory.outbox.OutboxService;
import com.marketplace.inventory.reservation.messaging.InventoryMessagingConfiguration;
import com.marketplace.inventory.reservation.messaging.InventoryReservationResult;
import com.marketplace.inventory.reservation.messaging.ReserveInventoryCommand;
import com.marketplace.inventory.reservation.messaging.ReserveInventoryCommand.Item;
import com.marketplace.inventory.reservation.repository.InventoryReservation;
import com.marketplace.inventory.reservation.repository.InventoryReservationRepository;
import com.marketplace.inventory.stock.repository.InventoryItem;
import com.marketplace.inventory.stock.repository.InventoryItemRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryReservationService {

    private static final String COMMAND_EVENT_TYPE = "ReserveInventoryCommand.v1";
    private static final String RESERVED_EVENT_TYPE = "InventoryReservedEvent.v1";
    private static final String REJECTED_EVENT_TYPE = "InventoryReservationRejectedEvent.v1";

    private final JdbcTemplate jdbcTemplate;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryReservationRepository reservationRepository;
    private final OutboxService outboxService;
    private final InventoryAvailabilityOutboxService availabilityOutboxService;

    public InventoryReservationService(
            JdbcTemplate jdbcTemplate,
            InventoryItemRepository inventoryItemRepository,
            InventoryReservationRepository reservationRepository,
            OutboxService outboxService,
            InventoryAvailabilityOutboxService availabilityOutboxService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.inventoryItemRepository = inventoryItemRepository;
        this.reservationRepository = reservationRepository;
        this.outboxService = outboxService;
        this.availabilityOutboxService = availabilityOutboxService;
    }

    @Transactional
    public void handle(ReserveInventoryCommand command) {
        if (!claim(command.eventId())) {
            return;
        }
        var existing = reservationRepository.findByOrderId(command.orderId());
        if (existing.isPresent()) {
            publishResult(existing.get());
            return;
        }

        InventoryReservation reservation = new InventoryReservation(command.orderId());
        command.items().forEach(item -> reservation.addItem(item.productId(), item.quantity()));

        var productIds = command.items().stream()
            .map(Item::productId)
            .sorted(Comparator.comparing(UUID::toString))
            .toList();
        Map<UUID, InventoryItem> stockByProduct = inventoryItemRepository.findAllByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(InventoryItem::getProductId, Function.identity()));

        String rejectionReason = findRejectionReason(command, stockByProduct);
        if (rejectionReason == null) {
            command.items().stream()
                    .sorted(Comparator.comparing(item -> item.productId().toString()))
                    .forEach(item -> stockByProduct.get(item.productId()).reserve(item.quantity()));
            availabilityOutboxService.enqueueAll(stockByProduct.values());
            reservation.markReserved();
        } else {
            reservation.reject(rejectionReason);
        }

        reservationRepository.saveAndFlush(reservation);
        publishResult(reservation);
    }

    private String findRejectionReason(
            ReserveInventoryCommand command,
            Map<UUID, InventoryItem> stockByProduct
    ) {
        for (var requested : command.items()) {
            InventoryItem stock = stockByProduct.get(requested.productId());
            if (stock == null) {
                return "Product is not stocked. ID=" + requested.productId();
            }
            if (stock.getAvailableQuantity() < requested.quantity()) {
                return "Insufficient inventory. Product ID=" + requested.productId();
            }
        }
        return null;
    }

    private boolean claim(UUID eventId) {
        return jdbcTemplate.update("""
                INSERT INTO inbox_messages(event_id, event_type, processed_at)
                VALUES (?, ?, now())
                ON CONFLICT (event_id) DO NOTHING
                """, eventId, COMMAND_EVENT_TYPE) == 1;
    }

    private void publishResult(InventoryReservation reservation) {
        boolean reserved = reservation.getStatus() != ReservationStatus.REJECTED
                && reservation.getStatus() != ReservationStatus.PENDING;
        InventoryReservationResult result = new InventoryReservationResult(
                UUID.randomUUID(),
                reservation.getOrderId(),
                reservation.getPublicId(),
                reserved ? "RESERVED" : "REJECTED",
                reservation.getFailureReason(),
                Instant.now()
        );
        outboxService.enqueue(
                result.eventId(),
                reserved ? RESERVED_EVENT_TYPE : REJECTED_EVENT_TYPE,
                reserved
                        ? InventoryMessagingConfiguration.INVENTORY_RESERVED_EVENT_ROUTING_KEY
                        : InventoryMessagingConfiguration.INVENTORY_RESERVATION_REJECTED_EVENT_ROUTING_KEY,
                result
        );
    }
}
