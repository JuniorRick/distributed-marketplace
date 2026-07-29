package com.marketplace.orders.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketplace.orders.cart.CartClient;
import com.marketplace.orders.order.repository.Order;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderWorkflowServiceTest {

    @Mock
    private CartClient cartClient;

    @Mock
    private OrderPersistenceService orderPersistenceService;

    @InjectMocks
    private OrderWorkflowService orderWorkflowService;

    @Test
    void createsPendingOrderBeforeCheckoutAndConfirmsIt() {
        UUID cartId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        var cart = activeCart(cartId, customerId);
        Order pending = pendingOrder(cartId, customerId);
        Order confirmed = pendingOrder(cartId, customerId);
        confirmed.confirm();

        when(orderPersistenceService.findBySourceCartId(cartId)).thenReturn(Optional.empty());
        when(cartClient.getCart(cartId)).thenReturn(cart);
        when(orderPersistenceService.createPending(cart)).thenReturn(pending);
        when(cartClient.checkout(cartId)).thenReturn(checkedOutCart(cart));
        when(orderPersistenceService.confirm(pending.getPublicId())).thenReturn(confirmed);

        var result = orderWorkflowService.createFromCart(cartId);

        assertThat(result.created()).isTrue();
        assertThat(result.order().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderPersistenceService).createPending(cart);
        verify(cartClient).checkout(cartId);
    }

    @Test
    void retryResumesExistingPendingOrderWithoutRebuildingSnapshot() {
        UUID cartId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        var cart = activeCart(cartId, customerId);
        Order pending = pendingOrder(cartId, customerId);
        Order confirmed = pendingOrder(cartId, customerId);
        confirmed.confirm();

        when(orderPersistenceService.findBySourceCartId(cartId)).thenReturn(Optional.of(pending));
        when(cartClient.checkout(cartId)).thenReturn(checkedOutCart(cart));
        when(orderPersistenceService.confirm(pending.getPublicId())).thenReturn(confirmed);

        var result = orderWorkflowService.createFromCart(cartId);

        assertThat(result.created()).isFalse();
        assertThat(result.order().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(cartClient, never()).getCart(cartId);
        verify(orderPersistenceService, never()).createPending(cart);
    }

    @Test
    void retryReturnsExistingConfirmedOrderWithoutCallingCart() {
        UUID cartId = UUID.randomUUID();
        Order confirmed = pendingOrder(cartId, UUID.randomUUID());
        confirmed.confirm();
        when(orderPersistenceService.findBySourceCartId(cartId)).thenReturn(Optional.of(confirmed));

        var result = orderWorkflowService.createFromCart(cartId);

        assertThat(result.created()).isFalse();
        assertThat(result.order()).isSameAs(confirmed);
        verify(cartClient, never()).checkout(cartId);
    }

    private static Order pendingOrder(UUID cartId, UUID customerId) {
        Order order = new Order(cartId, customerId, "USD");
        order.addItem(
                UUID.randomUUID(), "BOOK-001", "Book", new BigDecimal("10.00"), "USD", 1
        );
        return order;
    }

    private static CartClient.CartSnapshot activeCart(UUID cartId, UUID customerId) {
        var money = new CartClient.MoneySnapshot(new BigDecimal("10.00"), "USD");
        var item = new CartClient.CartItemSnapshot(
                UUID.randomUUID(), UUID.randomUUID(), "BOOK-001", "Book", money, 1, money
        );
        return new CartClient.CartSnapshot(
                cartId, customerId, "ACTIVE", List.of(item), money
        );
    }

    private static CartClient.CartSnapshot checkedOutCart(CartClient.CartSnapshot cart) {
        return new CartClient.CartSnapshot(
                cart.id(), cart.customerId(), "CHECKED_OUT", cart.items(), cart.subtotal()
        );
    }
}
