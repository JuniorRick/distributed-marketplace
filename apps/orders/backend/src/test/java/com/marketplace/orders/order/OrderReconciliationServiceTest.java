package com.marketplace.orders.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketplace.orders.inventory.InventoryMessagingConfiguration;
import com.marketplace.orders.inventory.ReleaseInventoryCommand;
import com.marketplace.orders.messaging.OutboxService;
import com.marketplace.orders.order.repository.Order;
import com.marketplace.orders.order.repository.OrderRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class OrderReconciliationServiceTest {

    @Mock JdbcTemplate jdbcTemplate;
    @Mock OrderRepository orderRepository;
    @Mock OutboxService outboxService;
    @Mock Order order;
    @InjectMocks OrderPersistenceService service;

    @Test
    void exhaustedInventoryCommitStartsReleaseBeforeRefund() {
        UUID orderId = UUID.randomUUID();
        Instant staleBefore = Instant.now().minus(1, ChronoUnit.MINUTES);
        when(orderRepository.findByPublicId(orderId)).thenReturn(Optional.of(order));
        when(order.getUpdatedAt()).thenReturn(staleBefore.minusSeconds(1));
        when(order.getStatus()).thenReturn(OrderStatus.INVENTORY_COMMIT_PENDING);
        when(order.getReconciliationAttempts()).thenReturn(5);
        when(order.getPublicId()).thenReturn(orderId);

        service.reconcile(orderId, staleBefore, 5);

        verify(order).markInventoryReleaseForRefundPending(any(String.class));
        verify(outboxService).enqueue(
                any(UUID.class),
                eq("ReleaseInventoryCommand.v1"),
                eq(InventoryMessagingConfiguration.EXCHANGE),
                eq(InventoryMessagingConfiguration.RELEASE_INVENTORY_COMMAND_ROUTING_KEY),
                any(ReleaseInventoryCommand.class)
        );
    }
}
