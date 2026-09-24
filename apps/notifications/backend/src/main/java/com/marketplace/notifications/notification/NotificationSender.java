package com.marketplace.notifications.notification;

import java.util.UUID;

public interface NotificationSender {
    void send(UUID notificationId, UUID customerId, String message);
}
