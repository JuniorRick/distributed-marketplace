package com.marketplace.orders.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketplace.orders.checkout.CheckoutResultListener.CheckoutResult;
import com.marketplace.orders.inventory.InventoryMessagingConfiguration;
import com.marketplace.orders.inventory.InventoryResultListener.InventoryReservationResult;
import com.marketplace.orders.inventory.ReserveInventoryCommand;
import com.marketplace.orders.messaging.OutboxService;
import com.marketplace.orders.order.repository.Order;
import com.marketplace.orders.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class OrderPersistenceServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private OrderPersistenceService service;

    @Test
    void checkedOutCartRequestsInventoryWithoutConfirmingOrder() {
        Order order = pendingOrder();
        UUID eventId = UUID.randomUUID();
        when(jdbcTemplate.update(anyString(), eq(eventId), eq("CartCheckedOutEvent.v1")))
                .thenReturn(1);
        when(orderRepository.findByPublicId(order.getPublicId())).thenReturn(Optional.of(order));

        service.applyCheckoutResult(new CheckoutResult(
                eventId,
                order.getPublicId(),
                order.getSourceCartId(),
                "COMPLETED",
                null,
                Instant.now()
        ));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        ArgumentCaptor<ReserveInventoryCommand> command = ArgumentCaptor.forClass(
                ReserveInventoryCommand.class
        );
        verify(outboxService).enqueue(
                any(UUID.class),
                eq("ReserveInventoryCommand.v1"),
                eq(InventoryMessagingConfiguration.EXCHANGE),
                eq(InventoryMessagingConfiguration.RESERVE_INVENTORY_COMMAND_ROUTING_KEY),
                command.capture()
        );
        assertThat(command.getValue().items()).singleElement().satisfies(item -> {
            assertThat(item.productId()).isEqualTo(order.getItems().getFirst().getProductId());
            assertThat(item.quantity()).isEqualTo(2);
        });
    }

    @Test
    void reservedInventoryConfirmsPendingOrder() {
        Order order = pendingOrder();
        UUID eventId = UUID.randomUUID();
        when(jdbcTemplate.update(anyString(), eq(eventId), eq("InventoryReservedEvent.v1")))
                .thenReturn(1);
        when(orderRepository.findByPublicId(order.getPublicId())).thenReturn(Optional.of(order));

        service.applyInventoryResult(new InventoryReservationResult(
                eventId,
                order.getPublicId(),
                UUID.randomUUID(),
                "RESERVED",
                null,
                Instant.now()
        ));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(order);
    }

    private static Order pendingOrder() {
        Order order = new Order(UUID.randomUUID(), UUID.randomUUID(), "USD");
        order.addItem(
                UUID.randomUUID(),
                "BOOK-001",
                "Distributed Systems",
                new BigDecimal("29.90"),
                "USD",
                2
        );
        return order;
    }
}
