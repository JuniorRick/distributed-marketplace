package com.marketplace.orders.order;

public enum OrderStatus {
    PENDING,
    PAYMENT_PENDING,
    INVENTORY_COMMIT_PENDING,
    INVENTORY_RELEASE_PENDING,
    CONFIRMED,
    REJECTED
}
