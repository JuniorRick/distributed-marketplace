package com.marketplace.cart.cart;

import com.marketplace.cart.catalog.CatalogClient;
import com.marketplace.cart.cart.repository.Cart;
import com.marketplace.cart.cart.repository.CartRepository;
import com.marketplace.cart.shared.ConflictException;
import com.marketplace.cart.shared.NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CatalogClient catalogClient;
    private final CartRepository cartRepository;

    public CartService(CatalogClient catalogClient, CartRepository cartRepository) {
        this.catalogClient = catalogClient;
        this.cartRepository = cartRepository;
    }

    @Transactional
    public Cart createOrGetActiveCart(UUID customerId) {
        return cartRepository.findFirstByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
                .orElseGet(() -> cartRepository.save(new Cart(customerId)));
    }

    @Transactional
    public Cart addItem(UUID cartId, UUID productId, int quantity) {
        Cart cart = findCart(cartId);
        requireActive(cart);

        var product = catalogClient.getProduct(productId);
        if (!"ACTIVE".equals(product.status())) {
            throw new ConflictException("Archived products cannot be added to a cart");
        }
        if (!cart.acceptsCurrency(product.price().currency())) {
            throw new ConflictException("A cart cannot contain products in different currencies");
        }

        cart.addItem(
                product.id(),
                product.sku(),
                product.name(),
                product.price().amount(),
                product.price().currency(),
                quantity
        );
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart updateItemQuantity(UUID cartId, UUID itemId, int quantity) {
        Cart cart = findCart(cartId);
        requireActive(cart);

        if (!cart.updateItemQuantity(itemId, quantity)) {
            throw new NotFoundException("Cart item not found. ID=" + itemId);
        }
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart removeItem(UUID cartId, UUID itemId) {
        Cart cart = findCart(cartId);
        requireActive(cart);

        if (!cart.removeItem(itemId)) {
            throw new NotFoundException("Cart item not found. ID=" + itemId);
        }
        return cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public Cart getByUuid(UUID uuid) {
        return findCart(uuid);
    }

    private Cart findCart(UUID uuid) {
        return cartRepository.findByPublicId(uuid)
                .orElseThrow(() -> new NotFoundException("Cart not found. ID=" + uuid));
    }

    private void requireActive(Cart cart) {
        if (cart.getStatus() != CartStatus.ACTIVE) {
            throw new ConflictException("Items can only be changed in an active cart");
        }
    }
}