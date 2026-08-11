package com.marketplace.orders.order.repository;

import com.marketplace.orders.order.OrderStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<Order> findByPublicId(UUID publicId);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findBySourceCartId(UUID sourceCartId);

    List<Order> findTop100ByStatusInAndUpdatedAtBeforeOrderByUpdatedAtAsc(
            Collection<OrderStatus> statuses,
            Instant updatedBefore
    );
}
