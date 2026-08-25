package com.marketplace.cart.cart.api;

import com.marketplace.cart.cart.CartService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final CartApiMapper cartApiMapper;
    private final CartService cartService;
    private final Logger log = LoggerFactory.getLogger(CartController.class);

    public CartController(CartApiMapper cartApiMapper, CartService cartService) {
        this.cartApiMapper = cartApiMapper;
        this.cartService = cartService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CartResponse> getCart(@PathVariable UUID id) {
        log.info("GET /api/carts/{}", id);
        var cart = cartService.getByUuid(id);
        return ResponseEntity.ok(cartApiMapper.toResponse(cart));
    }

    @PostMapping
    public ResponseEntity<CartResponse> createCart(@Valid @RequestBody CreateCartRequest request) {
        var cart = cartService.createOrGetActiveCart(request.customerId());
        var response = cartApiMapper.toResponse(cart);
        return ResponseEntity
                .created(URI.create("/api/carts/" + response.id()))
                .body(response);
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<CartResponse> addItem(
            @PathVariable UUID id,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        var cart = cartService.addItem(id, request.productId(), request.quantity());
        return ResponseEntity.ok(cartApiMapper.toResponse(cart));
    }

    @PatchMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<CartResponse> updateItemQuantity(
            @PathVariable UUID cartId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateCartItemQuantityRequest request
    ) {
        var cart = cartService.updateItemQuantity(cartId, itemId, request.quantity());
        return ResponseEntity.ok(cartApiMapper.toResponse(cart));
    }

    @DeleteMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(
            @PathVariable UUID cartId,
            @PathVariable UUID itemId
    ) {
        var cart = cartService.removeItem(cartId, itemId);
        return ResponseEntity.ok(cartApiMapper.toResponse(cart));
    }

    @Deprecated
    @PostMapping("/{id}/checkout")
    public ResponseEntity<CartResponse> checkout(@PathVariable UUID id) {
        var cart = cartService.checkout(id);
        return ResponseEntity.ok(cartApiMapper.toResponse(cart));
    }
}