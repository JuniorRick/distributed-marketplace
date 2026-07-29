package com.marketplace.orders.order.api;

import com.marketplace.orders.order.repository.Order;
import com.marketplace.orders.order.repository.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class OrderApiMapper {

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getPublicId(),
                order.getSourceCartId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getItems().stream().map(this::toResponse).toList(),
                new MoneyResponse(order.getTotalAmount(), order.getCurrency()),
                order.getCreatedAt()
        );
    }

    private OrderItemResponse toResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getPublicId(),
                item.getProductId(),
                item.getProductSku(),
                item.getProductName(),
                new MoneyResponse(item.getUnitPriceAmount(), item.getCurrency()),
                item.getQuantity(),
                new MoneyResponse(item.getLineTotalAmount(), item.getCurrency())
        );
    }
}
