package com.marketplace.orders.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketplace.orders.checkout.CheckoutResultListener.CheckoutResult;
import com.marketplace.orders.inventory.InventoryMessagingConfiguration;
import com.marketplace.orders.inventory.InventoryResultListener.InventoryReservationResult;
import com.marketplace.orders.inventory.CommitInventoryCommand;
import com.marketplace.orders.inventory.ReleaseInventoryCommand;
import com.marketplace.orders.inventory.ReserveInventoryCommand;
import com.marketplace.orders.outbox.OutboxService;
import com.marketplace.orders.order.repository.Order;
import com.marketplace.orders.order.repository.OrderRepository;
import com.marketplace.orders.order.messaging.OrderLifecycleEvent;
import com.marketplace.orders.order.messaging.OrderLifecycleMessagingConfiguration;
import com.marketplace.orders.payment.CapturePaymentCommand;
import com.marketplace.orders.payment.PaymentMessagingConfiguration;
import com.marketplace.orders.payment.PaymentResultListener.PaymentResult;
import com.marketplace.orders.payment.RefundPaymentCommand;
import com.marketplace.orders.shared.ConflictException;
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
        order.markCheckoutPending();
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

        assertThat(order.getStatus()).isEqualTo(OrderStatus.INVENTORY_RESERVATION_PENDING);
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
    void reservedInventoryRequestsPaymentWithoutConfirmingOrder() {
        Order order = pendingOrder();
        order.markInventoryReservationPending();
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

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        ArgumentCaptor<CapturePaymentCommand> command = ArgumentCaptor.forClass(
                CapturePaymentCommand.class
        );
        verify(outboxService).enqueue(
                any(UUID.class),
                eq("CapturePaymentCommand.v1"),
                eq(PaymentMessagingConfiguration.EXCHANGE),
                eq(PaymentMessagingConfiguration.CAPTURE_PAYMENT_COMMAND_ROUTING_KEY),
                command.capture()
        );
        assertThat(command.getValue().amount()).isEqualByComparingTo(order.getTotalAmount());
        assertThat(command.getValue().currency()).isEqualTo(order.getCurrency());
        assertThat(command.getValue().customerId()).isEqualTo(order.getCustomerId());
    }

    @Test
    void capturedPaymentRequestsInventoryCommit() {
        Order order = pendingOrder();
        order.markPaymentPending();
        UUID eventId = UUID.randomUUID();
        when(jdbcTemplate.update(anyString(), eq(eventId), eq("PaymentCapturedEvent.v1")))
                .thenReturn(1);
        when(orderRepository.findByPublicId(order.getPublicId())).thenReturn(Optional.of(order));

        service.applyPaymentResult(paymentResult(eventId, order, "CAPTURED", null));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.INVENTORY_COMMIT_PENDING);
        verify(outboxService).enqueue(
                any(UUID.class),
                eq("CommitInventoryCommand.v1"),
                eq(InventoryMessagingConfiguration.EXCHANGE),
                eq(InventoryMessagingConfiguration.COMMIT_INVENTORY_COMMAND_ROUTING_KEY),
                any(CommitInventoryCommand.class)
        );
    }

    @Test
    void failedPaymentRequestsInventoryRelease() {
        Order order = pendingOrder();
        order.markPaymentPending();
        UUID eventId = UUID.randomUUID();
        when(jdbcTemplate.update(anyString(), eq(eventId), eq("PaymentFailedEvent.v1")))
                .thenReturn(1);
        when(orderRepository.findByPublicId(order.getPublicId())).thenReturn(Optional.of(order));

        service.applyPaymentResult(paymentResult(eventId, order, "FAILED", "declined"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.INVENTORY_RELEASE_PENDING);
        assertThat(order.getFailureReason()).isEqualTo("declined");
        verify(outboxService).enqueue(
                any(UUID.class),
                eq("ReleaseInventoryCommand.v1"),
                eq(InventoryMessagingConfiguration.EXCHANGE),
                eq(InventoryMessagingConfiguration.RELEASE_INVENTORY_COMMAND_ROUTING_KEY),
                any(ReleaseInventoryCommand.class)
        );
    }

    @Test
    void committedInventoryConfirmsOrder() {
        Order order = pendingOrder();
        order.markPaymentPending();
        order.markInventoryCommitPending();
        applyInventoryResult(order, "COMMITTED", null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(outboxService).enqueue(
            any(UUID.class),
            eq("OrderConfirmedEvent.v1"),
            eq(OrderLifecycleMessagingConfiguration.EXCHANGE),
            eq(OrderLifecycleMessagingConfiguration.ORDER_CONFIRMED_EVENT_ROUTING_KEY),
            any(OrderLifecycleEvent.class)
        );
    }

    @Test
    void releasedInventoryRejectsOrderWithPaymentFailureReason() {
        Order order = pendingOrder();
        order.markPaymentPending();
        order.markInventoryReleasePending("declined");
        applyInventoryResult(order, "RELEASED", null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(order.getFailureReason()).isEqualTo("declined");
        verify(outboxService).enqueue(
            any(UUID.class),
            eq("OrderRejectedEvent.v1"),
            eq(OrderLifecycleMessagingConfiguration.EXCHANGE),
            eq(OrderLifecycleMessagingConfiguration.ORDER_REJECTED_EVENT_ROUTING_KEY),
            any(OrderLifecycleEvent.class)
        );
    }

    @Test
    void releasedCompensationRequestsRefundAndRefundResultCompletesOrder() {
        Order order = pendingOrder();
        order.markPaymentPending();
        order.markInventoryCommitPending();
        order.markInventoryReleaseForRefundPending("Inventory commit timed out");

        applyInventoryResult(order, "RELEASED", null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_PENDING);
        verify(outboxService).enqueue(
                any(UUID.class),
                eq("RefundPaymentCommand.v1"),
                eq(PaymentMessagingConfiguration.EXCHANGE),
                eq(PaymentMessagingConfiguration.REFUND_PAYMENT_COMMAND_ROUTING_KEY),
                any(RefundPaymentCommand.class)
        );

        UUID eventId = UUID.randomUUID();
        when(jdbcTemplate.update(anyString(), eq(eventId), eq("PaymentRefundedEvent.v1")))
                .thenReturn(1);
        service.applyPaymentResult(paymentResult(eventId, order, "REFUNDED", null));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUNDED);
        verify(outboxService).enqueue(
            any(UUID.class),
            eq("OrderRefundedEvent.v1"),
            eq(OrderLifecycleMessagingConfiguration.EXCHANGE),
            eq(OrderLifecycleMessagingConfiguration.ORDER_REFUNDED_EVENT_ROUTING_KEY),
            any(OrderLifecycleEvent.class)
        );
    }

    @Test
    void rejectsPaymentResultWithDifferentAmount() {
        Order order = pendingOrder();
        order.markPaymentPending();
        UUID eventId = UUID.randomUUID();
        when(jdbcTemplate.update(anyString(), eq(eventId), eq("PaymentCapturedEvent.v1")))
                .thenReturn(1);
        when(orderRepository.findByPublicId(order.getPublicId())).thenReturn(Optional.of(order));
        PaymentResult result = new PaymentResult(
                eventId,
                UUID.randomUUID(),
                order.getPublicId(),
                "CAPTURED",
                order.getTotalAmount().add(BigDecimal.ONE),
                order.getCurrency(),
                null,
                Instant.now()
        );

        assertThatThrownBy(() -> service.applyPaymentResult(result))
                .isInstanceOf(ConflictException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    }

    private void applyInventoryResult(Order order, String outcome, String reason) {
        UUID eventId = UUID.randomUUID();
        String eventType = "COMMITTED".equals(outcome)
                ? "InventoryCommittedEvent.v1"
                : "InventoryReleasedEvent.v1";
        when(jdbcTemplate.update(anyString(), eq(eventId), eq(eventType))).thenReturn(1);
        when(orderRepository.findByPublicId(order.getPublicId())).thenReturn(Optional.of(order));
        service.applyInventoryResult(new InventoryReservationResult(
                eventId, order.getPublicId(), UUID.randomUUID(), outcome, reason, Instant.now()
        ));
    }

    private static PaymentResult paymentResult(
            UUID eventId,
            Order order,
            String outcome,
            String reason
    ) {
        return new PaymentResult(
                eventId,
                UUID.randomUUID(),
                order.getPublicId(),
                outcome,
                order.getTotalAmount(),
                order.getCurrency(),
                reason,
                Instant.now()
        );
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
