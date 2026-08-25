package com.marketplace.catalog.product.repository;

import com.marketplace.catalog.product.ProductStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStatusOrderByNameAsc(ProductStatus status);

    Optional<Product> findByPublicId(UUID id);

    boolean existsBySku(String sku);
}
