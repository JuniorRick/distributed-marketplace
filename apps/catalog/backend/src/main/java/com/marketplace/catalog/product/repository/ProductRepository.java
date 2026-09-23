package com.marketplace.catalog.product.repository;

import com.marketplace.catalog.product.ProductStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
    select new com.marketplace.catalog.product.repository.ProductWithAvailability(
        p.publicId,
        p.sku,
        p.name,
        p.description,
        p.priceAmount,
        p.currency,
        p.status,
        coalesce(a.quantity, 0)
    )
    from Product p
    left join InventoryAvailability a
        on a.productId = p.publicId
    where p.status = :status
    order by p.name
    """)
    List<ProductWithAvailability> findWithAvailability(ProductStatus status);

    @Query("""
    select new com.marketplace.catalog.product.repository.ProductWithAvailability(
        p.publicId,
        p.sku,
        p.name,
        p.description,
        p.priceAmount,
        p.currency,
        p.status,
        coalesce(a.quantity, 0)
    )
    from Product p
    left join InventoryAvailability a
        on a.productId = p.publicId
    where p.publicId = :id
    """)
    Optional<ProductWithAvailability> findWithAvailabilityByPublicId(UUID id);

    boolean existsBySku(String sku);
}
