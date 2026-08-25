package com.marketplace.orders.order;

import com.marketplace.orders.cart.CartClient.CartSnapshot;
import com.marketplace.orders.checkout.CheckoutCartCommand;
import com.marketplace.orders.checkout.CheckoutMessagingConfiguration;
import com.marketplace.orders.checkout.CheckoutResultListener.CheckoutResult;
import com.marketplace.orders.inventory.CommitInventoryCommand;
import com.marketplace.orders.inventory.InventoryMessagingConfiguration;
import com.marketplace.orders.inventory.InventoryResultListener.InventoryReservationResult;
import com.marketplace.orders.inventory.ReleaseInventoryCommand;
import com.marketplace.orders.inventory.ReserveInventoryCommand;
import com.marketplace.orders.outbox.OutboxService;
import com.marketplace.orders.order.repository.Order;
import com.marketplace.orders.order.repository.OrderRepository;
import com.marketplace.orders.payment.CapturePaymentCommand;
import com.marketplace.orders.payment.PaymentMessagingConfiguration;
import com.marketplace.orders.payment.PaymentResultListener.PaymentResult;
import com.marketplace.orders.payment.RefundPaymentCommand;
import com.marketplace.orders.shared.ConflictException;
import com.marketplace.orders.shared.NotFoundException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPersistenceService {

    private final JdbcTemplate jdbcTemplate;
    private final OrderRepository orderRepository;
    private final OutboxService outboxService;

    public OrderPersistenceService(
        JdbcTemplate jdbcTemplate,
        OrderRepository orderRepository,
        OutboxService outboxService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.orderRepository = orderRepository;
        this.outboxService = outboxService;
    }

    @Transactional(readOnly = true)
    public Optional<Order> findBySourceCartId(UUID cartId) {
        return orderRepository.findBySourceCartId(cartId);
    }

    @Transactional(readOnly = true)
    public Order getByPublicId(UUID orderId) {
        return findOrder(orderId);
    }

    @Transactional
    public Order createPendingAndRequestCheckout(CartSnapshot cart) {
        Order order = new Order(cart.id(), cart.customerId(), cart.subtotal().currency());
        cart.items().forEach(item -> order.addItem(
            item.productId(), item.productSku(), item.productName(),
            item.unitPrice().amount(), item.unitPrice().currency(), item.quantity()
        ));
        if (order.getTotalAmount().compareTo(cart.subtotal().amount()) != 0) {
            throw new ConflictException("Cart subtotal does not match its item totals");
        }

        order.markCheckoutPending();
        Order saved = orderRepository.saveAndFlush(order);
        enqueueCheckout(saved);
        return saved;
    }

    @Transactional
    public void applyCheckoutResult(CheckoutResult event) {
        String eventType = switch (event.outcome()) {
            case "COMPLETED" -> "CartCheckedOutEvent.v1";
            case "REJECTED" -> "CartCheckoutRejectedEvent.v1";
            default -> throw new IllegalArgumentException("Unknown checkout outcome: " + event.outcome());
        };
        if (!claim(event.eventId(), eventType)) {
            return;
        }

        Order order = findOrder(event.orderId());
        if (!order.getSourceCartId().equals(event.cartId())) {
            throw new ConflictException("Checkout result does not match the order's cart");
        }
        if ("COMPLETED".equals(event.outcome())) {
            requestInventory(order);
        } else {
            order.reject(event.reason());
        }
        orderRepository.save(order);
    }

    @Transactional
    public void applyInventoryResult(InventoryReservationResult event) {
        String eventType = switch (event.outcome()) {
            case "RESERVED" -> "InventoryReservedEvent.v1";
            case "REJECTED" -> "InventoryReservationRejectedEvent.v1";
            case "COMMITTED" -> "InventoryCommittedEvent.v1";
            case "RELEASED" -> "InventoryReleasedEvent.v1";
            default -> throw new IllegalArgumentException("Unknown inventory outcome: " + event.outcome());
        };
        if (!claim(event.eventId(), eventType)) {
            return;
        }

        Order order = findOrder(event.orderId());
        switch (event.outcome()) {
            case "RESERVED" -> requestPayment(order);
            case "REJECTED" -> order.reject(event.reason());
            case "COMMITTED" -> {
                if (order.getStatus() == OrderStatus.INVENTORY_COMMIT_PENDING
                    || order.getStatus() == OrderStatus.INVENTORY_RELEASE_FOR_REFUND_PENDING) {
                    order.confirm();
                }
            }
            case "RELEASED" -> {
                if (order.getStatus() == OrderStatus.INVENTORY_RELEASE_PENDING) {
                    order.rejectAfterInventoryRelease();
                } else if (order.getStatus() == OrderStatus.INVENTORY_RELEASE_FOR_REFUND_PENDING) {
                    requestRefund(order, order.getFailureReason());
                }
            }
            default -> throw new IllegalArgumentException("Unknown inventory outcome: " + event.outcome());
        }
        orderRepository.save(order);
    }

    @Transactional
    public void applyPaymentResult(PaymentResult event) {
        String eventType = switch (event.outcome()) {
            case "CAPTURED" -> "PaymentCapturedEvent.v1";
            case "FAILED" -> "PaymentFailedEvent.v1";
            case "REFUNDED" -> "PaymentRefundedEvent.v1";
            case "REFUND_FAILED" -> "PaymentRefundFailedEvent.v1";
            default -> throw new IllegalArgumentException("Unknown payment outcome: " + event.outcome());
        };
        if (!claim(event.eventId(), eventType)) {
            return;
        }

        Order order = findOrder(event.orderId());
        if (order.getTotalAmount().compareTo(event.amount()) != 0
            || !order.getCurrency().equals(event.currency())) {
            throw new ConflictException("Payment result does not match the order total");
        }

        switch (event.outcome()) {
            case "CAPTURED" -> requestInventoryCommit(order);
            case "FAILED" -> requestInventoryRelease(order, event.reason());
            case "REFUNDED" -> order.markRefunded();
            case "REFUND_FAILED" -> order.recordRefundFailure(event.reason());
            default -> throw new IllegalArgumentException("Unknown payment outcome: " + event.outcome());
        }
        orderRepository.save(order);
    }

    @Transactional
    public void reconcile(UUID orderId, Instant staleBefore, int maxAttempts) {
        Order order = findOrder(orderId);
        if (order.getUpdatedAt() == null || !order.getUpdatedAt().isBefore(staleBefore)) {
            return;
        }
        if (order.getReconciliationAttempts() >= maxAttempts) {
            handleExhaustedReconciliation(order, maxAttempts);
            orderRepository.save(order);
            return;
        }

        switch (order.getStatus()) {
            case CHECKOUT_PENDING -> enqueueCheckout(order);
            case INVENTORY_RESERVATION_PENDING -> enqueueInventory(order);
            case PAYMENT_PENDING -> enqueuePayment(order);
            case INVENTORY_COMMIT_PENDING -> enqueueInventoryCommit(order);
            case INVENTORY_RELEASE_PENDING, INVENTORY_RELEASE_FOR_REFUND_PENDING -> enqueueInventoryRelease(order);
            case REFUND_PENDING -> enqueueRefund(order, order.getFailureReason());
            default -> {
                return;
            }
        }

        order.recordReconciliationAttempt();
        orderRepository.save(order);
    }

    private void handleExhaustedReconciliation(Order order, int maxAttempts) {
        String reason = "Saga phase " + order.getStatus()
            + " timed out after " + maxAttempts + " reconciliation attempts";
        if (order.getStatus() == OrderStatus.INVENTORY_COMMIT_PENDING) {
            order.markInventoryReleaseForRefundPending(reason);
            enqueueInventoryRelease(order);
        } else {
            order.markManualReview(reason);
        }
    }

    private void requestInventory(Order order) {
        if (order.getStatus() != OrderStatus.CHECKOUT_PENDING
            && order.getStatus() != OrderStatus.PENDING) {
            return;
        }
        enqueueInventory(order);
        order.markInventoryReservationPending();
    }

    private void requestPayment(Order order) {
        if (order.getStatus() != OrderStatus.INVENTORY_RESERVATION_PENDING
            && order.getStatus() != OrderStatus.PENDING) {
            return;
        }
        enqueuePayment(order);
        order.markPaymentPending();
    }

    private void requestInventoryCommit(Order order) {
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            return;
        }
        enqueueInventoryCommit(order);
        order.markInventoryCommitPending();
    }

    private void requestInventoryRelease(Order order, String reason) {
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            return;
        }
        enqueueInventoryRelease(order);
        order.markInventoryReleasePending(reason);
    }

    private void requestRefund(Order order, String reason) {
        enqueueRefund(order, reason);
        order.markRefundPending(reason);
    }

    private void enqueueCheckout(Order order) {
        CheckoutCartCommand command = new CheckoutCartCommand(
            UUID.randomUUID(), order.getPublicId(), order.getSourceCartId(), Instant.now()
        );
        outboxService.enqueue(
            command.eventId(), "CheckoutCartCommand.v1", CheckoutMessagingConfiguration.EXCHANGE,
            CheckoutMessagingConfiguration.CHECKOUT_CART_COMMAND_ROUTING_KEY, command
        );
    }

    private void enqueueInventory(Order order) {
        ReserveInventoryCommand command = new ReserveInventoryCommand(
            UUID.randomUUID(), order.getPublicId(), order.getItems().stream()
            .map(item -> new ReserveInventoryCommand.Item(
                item.getProductId(), item.getQuantity()
            )).toList(), Instant.now()
        );
        outboxService.enqueue(
            command.eventId(), "ReserveInventoryCommand.v1", InventoryMessagingConfiguration.EXCHANGE,
            InventoryMessagingConfiguration.RESERVE_INVENTORY_COMMAND_ROUTING_KEY, command
        );
    }

    private void enqueuePayment(Order order) {
        CapturePaymentCommand command = new CapturePaymentCommand(
            UUID.randomUUID(), order.getPublicId(), order.getCustomerId(),
            order.getTotalAmount(), order.getCurrency(), Instant.now()
        );
        outboxService.enqueue(
            command.eventId(), "CapturePaymentCommand.v1", PaymentMessagingConfiguration.EXCHANGE,
            PaymentMessagingConfiguration.CAPTURE_PAYMENT_COMMAND_ROUTING_KEY, command
        );
    }

    private void enqueueInventoryCommit(Order order) {
        CommitInventoryCommand command = new CommitInventoryCommand(
            UUID.randomUUID(), order.getPublicId(), Instant.now()
        );
        outboxService.enqueue(
            command.eventId(), "CommitInventoryCommand.v1", InventoryMessagingConfiguration.EXCHANGE,
            InventoryMessagingConfiguration.COMMIT_INVENTORY_COMMAND_ROUTING_KEY, command
        );
    }

    private void enqueueInventoryRelease(Order order) {
        ReleaseInventoryCommand command = new ReleaseInventoryCommand(
            UUID.randomUUID(), order.getPublicId(), Instant.now()
        );
        outboxService.enqueue(
            command.eventId(), "ReleaseInventoryCommand.v1", InventoryMessagingConfiguration.EXCHANGE,
            InventoryMessagingConfiguration.RELEASE_INVENTORY_COMMAND_ROUTING_KEY, command
        );
    }

    private void enqueueRefund(Order order, String reason) {
        RefundPaymentCommand command = new RefundPaymentCommand(
            UUID.randomUUID(), order.getPublicId(), reason, Instant.now()
        );
        outboxService.enqueue(
            command.eventId(), "RefundPaymentCommand.v1", PaymentMessagingConfiguration.EXCHANGE,
            PaymentMessagingConfiguration.REFUND_PAYMENT_COMMAND_ROUTING_KEY, command
        );
    }

    private Order findOrder(UUID orderId) {
        return orderRepository.findByPublicId(orderId)
            .orElseThrow(() -> new NotFoundException("Order not found. ID=" + orderId));
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
}
