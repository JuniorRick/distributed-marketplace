package com.marketplace.orders.order.api;

import com.marketplace.orders.order.OrderPersistenceService;
import com.marketplace.orders.order.OrderWorkflowService;
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
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        var result = orderWorkflowService.createFromCart(request.cartId());
        var response = orderApiMapper.toResponse(result.order());
        if (!result.created()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity
                .created(URI.create("/api/orders/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
        var order = orderPersistenceService.getByPublicId(id);
        return ResponseEntity.ok(orderApiMapper.toResponse(order));
    }
}
