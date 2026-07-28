package com.marketplace.cart.cart.repository;

import com.marketplace.cart.cart.CartStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<Cart> findByPublicId(UUID publicId);

    @EntityGraph(attributePaths = "items")
    Optional<Cart> findFirstByCustomerIdAndStatus(UUID customerId, CartStatus status);
}