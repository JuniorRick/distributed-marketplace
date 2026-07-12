package com.marketplace.catalog.product.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank @Size(max = 64) String sku,
        @NotBlank @Size(max = 160) String name,
        @NotBlank String description,
        @NotNull @DecimalMin("0.01") BigDecimal priceAmount,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency
) {
}
