package com.marketplace.customers.customer.api;

public record UpdateNotificationPreferencesRequest(boolean emailEnabled, boolean smsEnabled) {
}
