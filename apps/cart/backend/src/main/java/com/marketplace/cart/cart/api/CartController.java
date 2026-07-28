package com.marketplace.cart.cart.api;

import com.marketplace.cart.cart.CartService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

    public CartController(CartApiMapper cartApiMapper, CartService cartService) {
        this.cartApiMapper = cartApiMapper;
        this.cartService = cartService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CartResponse> getCart(@PathVariable UUID id) {
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
}