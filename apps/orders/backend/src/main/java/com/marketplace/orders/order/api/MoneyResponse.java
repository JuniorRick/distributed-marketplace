package com.marketplace.orders.order.api;

import java.math.BigDecimal;

public record MoneyResponse(BigDecimal amount, String currency) {
}
