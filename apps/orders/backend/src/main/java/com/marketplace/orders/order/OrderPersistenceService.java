package com.marketplace.orders.order;

import com.marketplace.orders.cart.CartClient.CartSnapshot;
import com.marketplace.orders.checkout.CheckoutMessagingConfiguration;
import com.marketplace.orders.checkout.CheckoutCartCommand;
import com.marketplace.orders.checkout.CheckoutResultListener.CheckoutResult;
import com.marketplace.orders.messaging.OutboxService;
import com.marketplace.orders.order.repository.Order;
import com.marketplace.orders.order.repository.OrderRepository;
import com.marketplace.orders.shared.ConflictException;
import com.marketplace.orders.shared.NotFoundException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
        return orderRepository.findByPublicId(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found. ID=" + orderId));
    }

    @Transactional
    public Order createPendingAndRequestCheckout(CartSnapshot cart) {
        Order order = new Order(cart.id(), cart.customerId(), cart.subtotal().currency());
        cart.items().forEach(item -> order.addItem(
                item.productId(),
                item.productSku(),
                item.productName(),
                item.unitPrice().amount(),
                item.unitPrice().currency(),
                item.quantity()
        ));

        if (order.getTotalAmount().compareTo(cart.subtotal().amount()) != 0) {
            throw new ConflictException("Cart subtotal does not match its item totals");
        }

        Order saved = orderRepository.saveAndFlush(order);
        CheckoutCartCommand event = new CheckoutCartCommand(
                UUID.randomUUID(),
                saved.getPublicId(),
                saved.getSourceCartId(),
                Instant.now()
        );
        outboxService.enqueue(
                event.eventId(),
                "CheckoutCartCommand.v1",
                CheckoutMessagingConfiguration.CHECKOUT_CART_COMMAND_ROUTING_KEY,
                event
        );
        return saved;
    }

    @Transactional
    public void applyCheckoutResult(CheckoutResult event) {
        String eventType = "COMPLETED".equals(event.outcome())
                ? "CartCheckedOutEvent.v1"
                : "CartCheckoutRejectedEvent.v1";
        if (!claim(event.eventId(), eventType)) {
            return;
        }

        Order order = orderRepository.findByPublicId(event.orderId())
                .orElseThrow(() -> new NotFoundException("Order not found. ID=" + event.orderId()));
        if (!order.getSourceCartId().equals(event.cartId())) {
            throw new ConflictException("Checkout result does not match the order's cart");
        }

        if ("COMPLETED".equals(event.outcome())) {
            order.confirm();
        } else if ("REJECTED".equals(event.outcome())) {
            order.reject(event.reason());
        } else {
            throw new IllegalArgumentException("Unknown checkout outcome: " + event.outcome());
        }
        orderRepository.save(order);
    }

    private boolean claim(UUID eventId, String eventType) {
        return jdbcTemplate.update("""
                INSERT INTO inbox_messages(event_id, event_type, processed_at)
                VALUES (?, ?, now())
                ON CONFLICT (event_id) DO NOTHING
                """, eventId, eventType) == 1;
    }
}
