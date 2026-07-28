package com.marketplace.cart.cart.repository;

import static org.assertj.core.api.Assertions.assertThat;

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
}
