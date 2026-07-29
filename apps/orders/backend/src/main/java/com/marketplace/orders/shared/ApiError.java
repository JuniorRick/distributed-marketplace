package com.marketplace.orders.shared;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> details
) {
    public static ApiError of(int status, String error, String message) {
        return new ApiError(Instant.now(), status, error, message, Map.of());
    }

    public static ApiError withDetails(int status, String error, String message, Map<String, String> details) {
        return new ApiError(Instant.now(), status, error, message, details);
    }
}
