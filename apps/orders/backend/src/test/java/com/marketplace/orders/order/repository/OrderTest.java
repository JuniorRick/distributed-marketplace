package com.marketplace.orders.order.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketplace.orders.order.OrderStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    void preservesImmutableItemSnapshotAndConfirms() {
        Order order = new Order(UUID.randomUUID(), UUID.randomUUID(), "USD");
        order.addItem(
                UUID.randomUUID(), "BOOK-001", "Distributed Systems",
                new BigDecimal("29.90"), "USD", 2
        );

        order.confirm();
        order.confirm();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("59.80");
        assertThat(order.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getProductName()).isEqualTo("Distributed Systems");
            assertThat(item.getLineTotalAmount()).isEqualByComparingTo("59.80");
        });
    }

    @Test
    void rejectsPendingOrderWithReason() {
        Order order = new Order(UUID.randomUUID(), UUID.randomUUID(), "USD");
        order.addItem(
                UUID.randomUUID(), "BOOK-001", "Distributed Systems",
                new BigDecimal("29.90"), "USD", 1
        );

        order.reject("Cart is no longer active");
        order.reject("Ignored duplicate");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(order.getFailureReason()).isEqualTo("Cart is no longer active");
    }
}
