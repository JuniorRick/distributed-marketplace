package com.marketplace.notifications.recipient;

import java.time.Instant;
import java.util.UUID;

public record CustomerProfileEvent(
    UUID eventId,
    UUID customerId,
    String email,
    String phone,
    boolean emailNotificationsEnabled,
    boolean smsNotificationsEnabled,
    long version,
    Instant occurredAt
) {
}
