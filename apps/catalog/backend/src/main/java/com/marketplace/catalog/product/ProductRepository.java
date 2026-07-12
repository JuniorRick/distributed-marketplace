package com.marketplace.catalog.product;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByStatusOrderByNameAsc(ProductStatus status);

    boolean existsBySku(String sku);
}
