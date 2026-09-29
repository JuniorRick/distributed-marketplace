package com.marketplace.orders.order.api;

import com.marketplace.orders.order.OrderPersistenceService;
import com.marketplace.orders.order.OrderWorkflowService;
import com.marketplace.orders.shared.NotFoundException;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderApiMapper orderApiMapper;
    private final OrderPersistenceService orderPersistenceService;
    private final OrderWorkflowService orderWorkflowService;

    public OrderController(
            OrderApiMapper orderApiMapper,
            OrderPersistenceService orderPersistenceService,
            OrderWorkflowService orderWorkflowService
    ) {
        this.orderApiMapper = orderApiMapper;
        this.orderPersistenceService = orderPersistenceService;
        this.orderWorkflowService = orderWorkflowService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        var result = jwt == null
            ? orderWorkflowService.createFromCart(request.cartId())
            : orderWorkflowService.createFromCart(
                request.cartId(), UUID.fromString(jwt.getSubject()), jwt.getTokenValue()
            );
        var response = orderApiMapper.toResponse(result.order());
        if (response.status().isTerminal()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity
                .accepted()
                .location(java.net.URI.create("/api/orders/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        var order = orderPersistenceService.getByPublicId(id);
        if (jwt != null && !order.getCustomerId().equals(UUID.fromString(jwt.getSubject()))) {
            throw new NotFoundException("Order %s was not found".formatted(id));
        }
        return ResponseEntity.ok(orderApiMapper.toResponse(order));
    }
}
