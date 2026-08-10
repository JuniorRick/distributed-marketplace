package com.marketplace.orders.order.repository;

import com.marketplace.orders.order.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;

    @Column(name = "source_cart_id", nullable = false, unique = true)
    private UUID sourceCartId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Order() {
    }

    public Order(UUID sourceCartId, UUID customerId, String currency) {
        this.publicId = UUID.randomUUID();
        this.sourceCartId = sourceCartId;
        this.customerId = customerId;
        this.currency = currency;
        this.status = OrderStatus.PENDING;
        this.totalAmount = BigDecimal.ZERO;
    }

    public void addItem(
            UUID productId,
            String productSku,
            String productName,
            BigDecimal unitPriceAmount,
            String currency,
            int quantity
    ) {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Items cannot be changed after order confirmation");
        }
        if (!this.currency.equals(currency)) {
            throw new IllegalArgumentException("Order items must use the order currency");
        }

        OrderItem item = new OrderItem(
                this, productId, productSku, productName, unitPriceAmount, currency, quantity
        );
        items.add(item);
        totalAmount = totalAmount.add(item.getLineTotalAmount());
    }

    public void confirm() {
        if (status == OrderStatus.CONFIRMED) {
            return;
        }
        requireStatus(
            OrderStatus.INVENTORY_COMMIT_PENDING,
            "Only an order awaiting inventory commit can be confirmed"
        );
        status = OrderStatus.CONFIRMED;
    }

    public void markPaymentPending() {
        requireStatus(OrderStatus.PENDING, "Only a pending order can await payment");
        status = OrderStatus.PAYMENT_PENDING;
    }

    public void markInventoryCommitPending() {
        requireStatus(
            OrderStatus.PAYMENT_PENDING,
            "Only an order awaiting payment can await inventory commit"
        );
        status = OrderStatus.INVENTORY_COMMIT_PENDING;
    }

    public void markInventoryReleasePending(String reason) {
        requireStatus(
            OrderStatus.PAYMENT_PENDING,
            "Only an order awaiting payment can await inventory release"
        );
        requireReason(reason);
        status = OrderStatus.INVENTORY_RELEASE_PENDING;
        failureReason = reason;
    }

    public void rejectAfterInventoryRelease() {
        if (status == OrderStatus.REJECTED) {
            return;
        }
        requireStatus(
            OrderStatus.INVENTORY_RELEASE_PENDING,
            "Only an order awaiting inventory release can be rejected"
        );
        status = OrderStatus.REJECTED;
    }

    public void reject(String reason) {
        if (status == OrderStatus.REJECTED) {
            return;
        }
        requireStatus(OrderStatus.PENDING, "Only a pending order can be rejected");
        requireReason(reason);
        status = OrderStatus.REJECTED;
        failureReason = reason;
    }

    private void requireStatus(OrderStatus expected, String message) {
        if (status != expected) {
            throw new IllegalStateException(message);
        }
    }

    private void requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A rejection reason is required");
        }
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public UUID getSourceCartId() {
        return sourceCartId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public long getVersion() {
        return version;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
