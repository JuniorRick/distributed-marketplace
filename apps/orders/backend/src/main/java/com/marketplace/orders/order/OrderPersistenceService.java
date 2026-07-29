package com.marketplace.orders.order;

import com.marketplace.orders.cart.CartClient.CartSnapshot;
import com.marketplace.orders.order.repository.Order;
import com.marketplace.orders.order.repository.OrderRepository;
import com.marketplace.orders.shared.ConflictException;
import com.marketplace.orders.shared.NotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPersistenceService {

    private final OrderRepository orderRepository;

    public OrderPersistenceService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Order> findBySourceCartId(UUID cartId) {
        return orderRepository.findBySourceCartId(cartId);
    }

    @Transactional(readOnly = true)
    public Order getByPublicId(UUID orderId) {
        return orderRepository.findByPublicId(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found. ID=" + orderId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order createPending(CartSnapshot cart) {
        Order order = new Order(cart.id(), cart.customerId(), cart.subtotal().currency());
        cart.items().forEach(item -> order.addItem(
                item.productId(),
                item.productSku(),
                item.productName(),
                item.unitPrice().amount(),
                item.unitPrice().currency(),
                item.quantity()
        ));

        if (order.getTotalAmount().compareTo(cart.subtotal().amount()) != 0) {
            throw new ConflictException("Cart subtotal does not match its item totals");
        }
        return orderRepository.saveAndFlush(order);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order confirm(UUID orderId) {
        Order order = orderRepository.findByPublicId(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found. ID=" + orderId));
        order.confirm();
        return orderRepository.save(order);
    }
}
