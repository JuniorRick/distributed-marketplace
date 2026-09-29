package com.marketplace.cart.cart.api;

import com.marketplace.cart.cart.CartService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
    public ResponseEntity<CartResponse> getCart(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        log.info("GET /api/carts/{}", id);
        var cart = jwt == null
            ? cartService.getByUuid(id)
            : cartService.getByUuid(id, UUID.fromString(jwt.getSubject()));
        return ResponseEntity.ok(cartApiMapper.toResponse(cart));
    }

    @PostMapping
    public ResponseEntity<CartResponse> createCart(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody(required = false) CreateCartRequest request
    ) {
        UUID customerId = jwt != null ? UUID.fromString(jwt.getSubject()) : requireCustomerId(request);
        var cart = cartService.createOrGetActiveCart(customerId);
        var response = cartApiMapper.toResponse(cart);
        return ResponseEntity
                .created(URI.create("/api/carts/" + response.id()))
                .body(response);
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<CartResponse> addItem(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        var cart = cartService.addItem(id, subject(jwt), request.productId(), request.quantity());
        return ResponseEntity.ok(cartApiMapper.toResponse(cart));
    }

    @PatchMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<CartResponse> updateItemQuantity(
            @PathVariable UUID cartId,
            @PathVariable UUID itemId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateCartItemQuantityRequest request
    ) {
        var cart = cartService.updateItemQuantity(cartId, subject(jwt), itemId, request.quantity());
        return ResponseEntity.ok(cartApiMapper.toResponse(cart));
    }

    @DeleteMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(
            @PathVariable UUID cartId,
            @PathVariable UUID itemId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var cart = cartService.removeItem(cartId, subject(jwt), itemId);
        return ResponseEntity.ok(cartApiMapper.toResponse(cart));
    }

    @Deprecated
    @PostMapping("/{id}/checkout")
    public ResponseEntity<CartResponse> checkout(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        if (jwt != null) {
            cartService.getByUuid(id, subject(jwt));
        }
        var cart = cartService.checkout(id);
        return ResponseEntity.ok(cartApiMapper.toResponse(cart));
    }

    private static UUID subject(Jwt jwt) {
        return jwt == null ? null : UUID.fromString(jwt.getSubject());
    }

    private static UUID requireCustomerId(CreateCartRequest request) {
        if (request == null || request.customerId() == null) {
            throw new IllegalArgumentException("customerId is required when security is disabled");
        }
        return request.customerId();
    }
}
