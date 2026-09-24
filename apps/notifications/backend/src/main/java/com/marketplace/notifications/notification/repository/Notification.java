package com.marketplace.notifications.notification.repository;

import com.marketplace.notifications.notification.NotificationStatus;
import com.marketplace.notifications.notification.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;
    @Column(name = "source_event_id", nullable = false, unique = true)
    private UUID sourceEventId;
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationType type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationStatus status;
    @Column(nullable = false, length = 500)
    private String message;
    @Column(nullable = false)
    private int attempts;
    @Column(name = "last_error", length = 500)
    private String lastError;
    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;
    @Column(name = "locked_at")
    private Instant lockedAt;
    @Column(name = "sent_at")
    private Instant sentAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
    }

    public Notification(UUID sourceEventId, UUID orderId, UUID customerId, NotificationType type, String message) {
        this.publicId = UUID.randomUUID();
        this.sourceEventId = sourceEventId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.type = type;
        this.status = NotificationStatus.PENDING;
        this.message = message;
        this.nextAttemptAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (publicId == null) publicId = UUID.randomUUID();
        if (status == null) status = NotificationStatus.PENDING;
        if (nextAttemptAt == null) nextAttemptAt = now;
        createdAt = now;
    }

    public UUID getPublicId() { return publicId; }
    public UUID getSourceEventId() { return sourceEventId; }
    public UUID getOrderId() { return orderId; }
    public UUID getCustomerId() { return customerId; }
    public NotificationType getType() { return type; }
    public NotificationStatus getStatus() { return status; }
    public String getMessage() { return message; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }
    public Instant getSentAt() { return sentAt; }
    public Instant getCreatedAt() { return createdAt; }
}
