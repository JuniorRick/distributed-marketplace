package com.marketplace.orders.payment;

import com.marketplace.orders.order.OrderPersistenceService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class PaymentResultListener {

    private final ObjectMapper objectMapper;
    private final OrderPersistenceService orderPersistenceService;

    public PaymentResultListener(
            ObjectMapper objectMapper,
            OrderPersistenceService orderPersistenceService
    ) {
        this.objectMapper = objectMapper;
        this.orderPersistenceService = orderPersistenceService;
    }

    @RabbitListener(queues = PaymentMessagingConfiguration.RESULT_QUEUE)
    public void handle(String payload) throws JacksonException {
        orderPersistenceService.applyPaymentResult(objectMapper.readValue(
                payload,
                PaymentResult.class
        ));
    }

    public record PaymentResult(
            UUID eventId,
            UUID paymentId,
            UUID orderId,
            String outcome,
            BigDecimal amount,
            String currency,
            String reason,
            Instant occurredAt
    ) {
    }
}
