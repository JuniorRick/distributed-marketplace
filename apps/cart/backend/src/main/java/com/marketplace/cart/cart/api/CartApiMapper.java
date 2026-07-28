package com.marketplace.cart.cart.api;

import com.marketplace.cart.cart.repository.Cart;
import com.marketplace.cart.cart.repository.CartItem;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CartApiMapper {

    public CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems()
                .stream()
                .map(this::toResponse)
                .toList();

        return new CartResponse(
                cart.getPublicId(),
                cart.getCustomerId(),
                cart.getStatus(),
                items,
                subtotal(cart.getItems())
        );
    }

    private CartItemResponse toResponse(CartItem item) {
        MoneyResponse unitPrice = new MoneyResponse(item.getUnitPriceAmount(), item.getCurrency());
        return new CartItemResponse(
                item.getPublicId(),
                item.getProductId(),
                item.getProductSku(),
                item.getProductName(),
                unitPrice,
                item.getQuantity(),
                lineTotal(item)
        );
    }

    private MoneyResponse lineTotal(CartItem item) {
        BigDecimal amount = item.getUnitPriceAmount()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
        return new MoneyResponse(amount, item.getCurrency());
    }

    private MoneyResponse subtotal(List<CartItem> items) {
        if (items.isEmpty()) {
            return new MoneyResponse(BigDecimal.ZERO, "USD");
        }

        String currency = items.get(0).getCurrency();
        BigDecimal amount = items.stream()
                .map(item -> item.getUnitPriceAmount().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new MoneyResponse(amount, currency);
    }
}