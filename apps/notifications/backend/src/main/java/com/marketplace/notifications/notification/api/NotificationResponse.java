package com.marketplace.notifications.notification.api;

import com.marketplace.notifications.notification.NotificationStatus;
import com.marketplace.notifications.notification.NotificationType;
import com.marketplace.notifications.notification.repository.Notification;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    UUID orderId,
    UUID customerId,
    NotificationType type,
    NotificationStatus status,
    String message,
    int attempts,
    String lastError,
    Instant sentAt,
    Instant createdAt
) {
    static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
            notification.getPublicId(),
            notification.getOrderId(),
            notification.getCustomerId(),
            notification.getType(),
            notification.getStatus(),
            notification.getMessage(),
            notification.getAttempts(),
            notification.getLastError(),
            notification.getSentAt(),
            notification.getCreatedAt()
        );
    }
}
