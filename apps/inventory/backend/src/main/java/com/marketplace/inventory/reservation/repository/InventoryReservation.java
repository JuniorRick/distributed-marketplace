package com.marketplace.inventory.reservation.repository;

import com.marketplace.inventory.reservation.ReservationStatus;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservations")
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReservationStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InventoryReservationItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InventoryReservation() {
    }

    public InventoryReservation(UUID orderId) {
        this.publicId = UUID.randomUUID();
        this.orderId = orderId;
        this.status = ReservationStatus.PENDING;
    }

    public void addItem(UUID productId, int quantity) {
        requireStatus(ReservationStatus.PENDING, "Items can only be added to a pending reservation");
        boolean duplicate = items.stream().anyMatch(item -> item.getProductId().equals(productId));
        if (duplicate) {
            throw new IllegalArgumentException("Reservation already contains this product");
        }
        items.add(new InventoryReservationItem(this, productId, quantity));
    }

    public void markReserved() {
        if (items.isEmpty()) {
            throw new IllegalStateException("An empty inventory reservation cannot be reserved");
        }
        requireStatus(ReservationStatus.PENDING, "Only a pending reservation can be reserved");
        status = ReservationStatus.RESERVED;
    }

    public void reject(String reason) {
        requireStatus(ReservationStatus.PENDING, "Only a pending reservation can be rejected");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A rejection reason is required");
        }
        status = ReservationStatus.REJECTED;
        failureReason = reason;
    }

    public void release() {
        requireStatus(ReservationStatus.RESERVED, "Only a reserved reservation can be released");
        status = ReservationStatus.RELEASED;
    }

    public void commit() {
        requireStatus(ReservationStatus.RESERVED, "Only a reserved reservation can be committed");
        status = ReservationStatus.COMMITTED;
    }

    private void requireStatus(ReservationStatus expected, String message) {
        if (status != expected) {
            throw new IllegalStateException(message);
        }
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        if (status == null) {
            status = ReservationStatus.PENDING;
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

    public UUID getOrderId() {
        return orderId;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public List<InventoryReservationItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
