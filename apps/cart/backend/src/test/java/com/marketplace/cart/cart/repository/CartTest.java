package com.marketplace.cart.cart.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CartTest {

    @Test
    void addingTheSameProductIncreasesItsQuantity() {
        Cart cart = new Cart(UUID.randomUUID());
        UUID productId = UUID.randomUUID();

        cart.addItem(
                productId,
                "BOOK-001",
                "Distributed Systems",
                new BigDecimal("29.90"),
                "USD",
                1
        );
        cart.addItem(
                productId,
                "BOOK-001",
                "Distributed Systems",
                new BigDecimal("29.90"),
                "USD",
                2
        );

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    void cartAcceptsOnlyItsExistingCurrency() {
        Cart cart = new Cart(UUID.randomUUID());
        cart.addItem(
                UUID.randomUUID(), "BOOK-001", "Book", new BigDecimal("10.00"), "USD", 1
        );

        assertThat(cart.acceptsCurrency("USD")).isTrue();
        assertThat(cart.acceptsCurrency("EUR")).isFalse();
    }

    @Test
    void changesItemQuantity() {
        Cart cart = new Cart(UUID.randomUUID());
        cart.addItem(
                UUID.randomUUID(), "BOOK-001", "Book", new BigDecimal("10.00"), "USD", 1
        );
        CartItem item = cart.getItems().get(0);

        boolean changed = cart.updateItemQuantity(item.getPublicId(), 4);

        assertThat(changed).isTrue();
        assertThat(item.getQuantity()).isEqualTo(4);
    }

    @Test
    void removesItem() {
        Cart cart = new Cart(UUID.randomUUID());
        cart.addItem(
                UUID.randomUUID(), "BOOK-001", "Book", new BigDecimal("10.00"), "USD", 1
        );
        CartItem item = cart.getItems().get(0);

        boolean removed = cart.removeItem(item.getPublicId());

        assertThat(removed).isTrue();
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void rejectsQuantityOutsideSupportedRange() {
        Cart cart = new Cart(UUID.randomUUID());
        cart.addItem(
                UUID.randomUUID(), "BOOK-001", "Book", new BigDecimal("10.00"), "USD", 1
        );
        UUID itemId = cart.getItems().get(0).getPublicId();

        assertThatThrownBy(() -> cart.updateItemQuantity(itemId, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cart item quantity must be between 1 and 99");
    }

    @Test
    void checksOutANonEmptyActiveCartIdempotently() {
        Cart cart = new Cart(UUID.randomUUID());
        cart.addItem(
                UUID.randomUUID(), "BOOK-001", "Book", new BigDecimal("10.00"), "USD", 1
        );

        cart.checkout();
        cart.checkout();

        assertThat(cart.getStatus()).isEqualTo(com.marketplace.cart.cart.CartStatus.CHECKED_OUT);
    }

    @Test
    void rejectsCheckoutForAnEmptyCart() {
        Cart cart = new Cart(UUID.randomUUID());

        assertThatThrownBy(cart::checkout)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("An empty cart cannot be checked out");
    }
}
