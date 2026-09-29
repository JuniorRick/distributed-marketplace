package com.marketplace.orders.order;

import com.marketplace.orders.cart.CartClient;
import com.marketplace.orders.order.repository.Order;
import com.marketplace.orders.shared.ConflictException;
import com.marketplace.orders.shared.NotFoundException;
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
        return createFromCart(cartId, null, null);
    }

    public OrderCreationResult createFromCart(UUID cartId, UUID authenticatedCustomerId, String bearerToken) {
        var existing = orderPersistenceService.findBySourceCartId(cartId);
        if (existing.isPresent()) {
            requireOwner(existing.get(), authenticatedCustomerId);
            return new OrderCreationResult(existing.get(), false);
        }

        var cart = bearerToken == null ? cartClient.getCart(cartId) : cartClient.getCart(cartId, bearerToken);
        if (authenticatedCustomerId != null && !authenticatedCustomerId.equals(cart.customerId())) {
            throw new NotFoundException("Cart %s was not found".formatted(cartId));
        }
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
            requireOwner(concurrent, authenticatedCustomerId);
            return new OrderCreationResult(concurrent, false);
        }
    }

    private static void requireOwner(Order order, UUID authenticatedCustomerId) {
        if (authenticatedCustomerId != null && !authenticatedCustomerId.equals(order.getCustomerId())) {
            throw new NotFoundException("Order was not found");
        }
    }

    public record OrderCreationResult(Order order, boolean created) {
    }
}
