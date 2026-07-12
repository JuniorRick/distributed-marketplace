package com.marketplace.catalog.product.api;

import java.math.BigDecimal;

public record MoneyResponse(BigDecimal amount, String currency) {
}
