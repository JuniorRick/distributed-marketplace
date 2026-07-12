package com.marketplace.catalog.product;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProductStatusTest {

    @Test
    void activeProductsAreVisibleInCatalog() {
        assertThat(ProductStatus.ACTIVE.name()).isEqualTo("ACTIVE");
    }
}
