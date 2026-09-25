package com.marketplace.customers.customer.api;

import com.marketplace.customers.customer.repository.Customer;
import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(
    UUID id,
    String email,
    String phone,
    boolean emailNotificationsEnabled,
    boolean smsNotificationsEnabled,
    long version,
    Instant createdAt,
    Instant updatedAt
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
            customer.getPublicId(),
            customer.getEmail(),
            customer.getPhone(),
            customer.isEmailNotificationsEnabled(),
            customer.isSmsNotificationsEnabled(),
            customer.getEventVersion(),
            customer.getCreatedAt(),
            customer.getUpdatedAt()
        );
    }
}
