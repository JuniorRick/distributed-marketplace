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
            return new OrderCreationResult(existing.get(), false);
        }

        var cart = cartClient.getCart(cartId);
        if (!"ACTIVE".equals(cart.status())) {
            throw new ConflictException("Only an active cart can create an order");
        }
        if (cart.items().isEmpty()) {
            throw new ConflictException("An empty cart cannot create an order");
        }

        try {
            Order pending = orderPersistenceService.createPendingAndRequestCheckout(cart);
            return new OrderCreationResult(pending, true);
        } catch (DataIntegrityViolationException exception) {
            Order concurrent = orderPersistenceService.findBySourceCartId(cartId)
                    .orElseThrow(() -> exception);
            return new OrderCreationResult(concurrent, false);
        }
    }

    public record OrderCreationResult(Order order, boolean created) {
    }
}
