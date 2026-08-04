package com.marketplace.orders.checkout;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.marketplace.orders.order.OrderPersistenceService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class CheckoutResultListener {

    private final ObjectMapper objectMapper;
    private final OrderPersistenceService orderPersistenceService;

    public CheckoutResultListener(
            ObjectMapper objectMapper,
            OrderPersistenceService orderPersistenceService
    ) {
        this.objectMapper = objectMapper;
        this.orderPersistenceService = orderPersistenceService;
    }

    @RabbitListener(queues = CheckoutMessagingConfiguration.RESULT_QUEUE)
    public void handle(String payload) throws JacksonException {
        CheckoutResult event = objectMapper.readValue(payload, CheckoutResult.class);
        orderPersistenceService.applyCheckoutResult(event);
    }

    public record CheckoutResult(
            UUID eventId,
            UUID orderId,
            UUID cartId,
            String outcome,
            String reason,
            Instant occurredAt
    ) {
    }
}
