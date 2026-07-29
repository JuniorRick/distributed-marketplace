package com.marketplace.orders.order;

import com.marketplace.orders.cart.CartClient;
import com.marketplace.orders.order.repository.Order;
import com.marketplace.orders.shared.ConflictException;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class OrderWorkflowService {

    private final CartClient cartClient;
    private final OrderPersistenceService orderPersistenceService;

    public OrderWorkflowService(
            CartClient cartClient,
            OrderPersistenceService orderPersistenceService
    ) {
        this.cartClient = cartClient;
        this.orderPersistenceService = orderPersistenceService;
    }

    public OrderCreationResult createFromCart(UUID cartId) {
        var existing = orderPersistenceService.findBySourceCartId(cartId);
        if (existing.isPresent()) {
            return complete(existing.get(), false);
        }

        var cart = cartClient.getCart(cartId);
        if (!"ACTIVE".equals(cart.status())) {
            throw new ConflictException("Only an active cart can create an order");
        }
        if (cart.items().isEmpty()) {
            throw new ConflictException("An empty cart cannot create an order");
        }

        Order pending;
        boolean created;
        try {
            pending = orderPersistenceService.createPending(cart);
            created = true;
        } catch (DataIntegrityViolationException exception) {
            pending = orderPersistenceService.findBySourceCartId(cartId)
                    .orElseThrow(() -> exception);
            created = false;
        }
        return complete(pending, created);
    }

    private OrderCreationResult complete(Order order, boolean created) {
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            return new OrderCreationResult(order, created);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ConflictException("Cancelled orders cannot be resumed");
        }

        var checkedOutCart = cartClient.checkout(order.getSourceCartId());
        if (!"CHECKED_OUT".equals(checkedOutCart.status())) {
            throw new ConflictException("Cart checkout did not complete");
        }

        return new OrderCreationResult(
                orderPersistenceService.confirm(order.getPublicId()),
                created
        );
    }

    public record OrderCreationResult(Order order, boolean created) {
    }
}
