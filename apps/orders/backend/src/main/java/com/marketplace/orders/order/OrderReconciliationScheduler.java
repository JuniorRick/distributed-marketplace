package com.marketplace.orders.order;

import com.marketplace.orders.order.repository.OrderRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderReconciliationScheduler {

    private static final EnumSet<OrderStatus> RECONCILABLE_STATUSES = EnumSet.of(
            OrderStatus.CHECKOUT_PENDING,
            OrderStatus.INVENTORY_RESERVATION_PENDING,
            OrderStatus.PAYMENT_PENDING,
            OrderStatus.INVENTORY_COMMIT_PENDING,
            OrderStatus.INVENTORY_RELEASE_PENDING,
            OrderStatus.INVENTORY_RELEASE_FOR_REFUND_PENDING,
            OrderStatus.REFUND_PENDING
    );

    private final OrderRepository orderRepository;
    private final OrderPersistenceService orderPersistenceService;
    private final Duration staleAfter;
    private final int maxAttempts;

    public OrderReconciliationScheduler(
            OrderRepository orderRepository,
            OrderPersistenceService orderPersistenceService,
            @Value("${marketplace.saga.reconciliation.stale-after:1m}") Duration staleAfter,
            @Value("${marketplace.saga.reconciliation.max-attempts:5}") int maxAttempts
    ) {
        if (staleAfter.isNegative() || staleAfter.isZero()) {
            throw new IllegalArgumentException("Saga stale-after must be positive");
        }
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("Saga max-attempts must be positive");
        }
        this.orderRepository = orderRepository;
        this.orderPersistenceService = orderPersistenceService;
        this.staleAfter = staleAfter;
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(
            initialDelayString = "${marketplace.saga.reconciliation.initial-delay:30s}",
            fixedDelayString = "${marketplace.saga.reconciliation.fixed-delay:30s}"
    )
    public void reconcileStaleOrders() {
        Instant staleBefore = Instant.now().minus(staleAfter);
        orderRepository.findTop100ByStatusInAndUpdatedAtBeforeOrderByUpdatedAtAsc(
                RECONCILABLE_STATUSES, staleBefore
        ).forEach(order -> orderPersistenceService.reconcile(
                order.getPublicId(), staleBefore, maxAttempts
        ));
    }
}
