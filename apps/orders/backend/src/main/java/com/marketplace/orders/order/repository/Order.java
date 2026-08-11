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
    @Column(nullable = false, length = 48)
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

    @Column(name = "reconciliation_attempts", nullable = false)
    private int reconciliationAttempts;

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
        requireOneOf(
            "Only an order settling committed inventory can be confirmed",
            OrderStatus.INVENTORY_COMMIT_PENDING,
            OrderStatus.INVENTORY_RELEASE_FOR_REFUND_PENDING,
            OrderStatus.MANUAL_REVIEW
        );
        transitionTo(OrderStatus.CONFIRMED);
    }

    public void markCheckoutPending() {
        requireStatus(OrderStatus.PENDING, "Only a new order can await checkout");
        transitionTo(OrderStatus.CHECKOUT_PENDING);
    }

    public void markInventoryReservationPending() {
        requireOneOf(
            "Only an order awaiting checkout can await inventory reservation",
            OrderStatus.PENDING,
            OrderStatus.CHECKOUT_PENDING
        );
        transitionTo(OrderStatus.INVENTORY_RESERVATION_PENDING);
    }

    public void markPaymentPending() {
        requireOneOf(
            "Only an order awaiting inventory can await payment",
            OrderStatus.PENDING,
            OrderStatus.INVENTORY_RESERVATION_PENDING
        );
        transitionTo(OrderStatus.PAYMENT_PENDING);
    }

    public void markInventoryCommitPending() {
        requireStatus(
            OrderStatus.PAYMENT_PENDING,
            "Only an order awaiting payment can await inventory commit"
        );
        transitionTo(OrderStatus.INVENTORY_COMMIT_PENDING);
    }

    public void markInventoryReleasePending(String reason) {
        requireStatus(
            OrderStatus.PAYMENT_PENDING,
            "Only an order awaiting payment can await inventory release"
        );
        requireReason(reason);
        transitionTo(OrderStatus.INVENTORY_RELEASE_PENDING);
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
        transitionTo(OrderStatus.REJECTED);
    }

    public void markInventoryReleaseForRefundPending(String reason) {
        requireStatus(
            OrderStatus.INVENTORY_COMMIT_PENDING,
            "Only an order awaiting inventory commit can start compensation"
        );
        requireReason(reason);
        transitionTo(OrderStatus.INVENTORY_RELEASE_FOR_REFUND_PENDING);
        failureReason = reason;
    }

    public void markRefundPending(String reason) {
        requireStatus(
            OrderStatus.INVENTORY_RELEASE_FOR_REFUND_PENDING,
            "Only an order with released inventory can await a refund"
        );
        requireReason(reason);
        transitionTo(OrderStatus.REFUND_PENDING);
        failureReason = reason;
    }

    public void markRefunded() {
        if (status == OrderStatus.REFUNDED) {
            return;
        }
        requireOneOf(
            "Only an order awaiting or manually reviewing a refund can be refunded",
            OrderStatus.REFUND_PENDING,
            OrderStatus.MANUAL_REVIEW
        );
        transitionTo(OrderStatus.REFUNDED);
    }

    public void recordRefundFailure(String reason) {
        requireStatus(OrderStatus.REFUND_PENDING, "Order is not awaiting a refund");
        requireReason(reason);
        failureReason = reason;
    }

    public void markManualReview(String reason) {
        if (status.isTerminal()) {
            throw new IllegalStateException("A terminal order cannot enter manual review");
        }
        requireReason(reason);
        transitionTo(OrderStatus.MANUAL_REVIEW);
        failureReason = reason;
    }

    public void recordReconciliationAttempt() {
        if (status.isTerminal() || status == OrderStatus.MANUAL_REVIEW) {
            throw new IllegalStateException("This order cannot be reconciled");
        }
        reconciliationAttempts++;
    }

    public void reject(String reason) {
        if (status == OrderStatus.REJECTED) {
            return;
        }
        requireOneOf(
            "Only an order awaiting checkout or inventory can be rejected",
            OrderStatus.PENDING,
            OrderStatus.CHECKOUT_PENDING,
            OrderStatus.INVENTORY_RESERVATION_PENDING
        );
        requireReason(reason);
        transitionTo(OrderStatus.REJECTED);
        failureReason = reason;
    }

    private void transitionTo(OrderStatus nextStatus) {
        status = nextStatus;
        resetReconciliation();
    }

    private void resetReconciliation() {
        reconciliationAttempts = 0;
    }

    private void requireStatus(OrderStatus expected, String message) {
        if (status != expected) {
            throw new IllegalStateException(message);
        }
    }

    private void requireOneOf(String message, OrderStatus... expectedStatuses) {
        for (OrderStatus expected : expectedStatuses) {
            if (status == expected) {
                return;
            }
        }
        throw new IllegalStateException(message);
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

    public int getReconciliationAttempts() {
        return reconciliationAttempts;
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
