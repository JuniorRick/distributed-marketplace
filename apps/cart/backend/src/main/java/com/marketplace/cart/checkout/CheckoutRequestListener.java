package com.marketplace.cart.checkout;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.marketplace.cart.cart.CartService;
import com.marketplace.cart.outbox.OutboxService;
import com.marketplace.cart.shared.ConflictException;
import com.marketplace.cart.shared.NotFoundException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CheckoutRequestListener {

    private final CartService cartService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxService outboxService;

    public CheckoutRequestListener(
            CartService cartService,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            OutboxService outboxService
    ) {
        this.cartService = cartService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.outboxService = outboxService;
    }

    @Transactional
    @RabbitListener(queues = CheckoutMessagingConfiguration.REQUEST_QUEUE)
    public void handle(String payload) throws JacksonException {
        CheckoutCartCommand event = objectMapper.readValue(payload, CheckoutCartCommand.class);
        if (!claim(event.eventId(), "CheckoutCartCommand.v1")) {
            return;
        }

        try {
            cartService.checkout(event.cartId());
            publishResult(
                    event,
                    "COMPLETED",
                    null,
                    CheckoutMessagingConfiguration.CART_CHECKED_OUT_EVENT_ROUTING_KEY
            );
        } catch (NotFoundException | ConflictException exception) {
            publishResult(
                    event,
                    "REJECTED",
                    exception.getMessage(),
                    CheckoutMessagingConfiguration.CART_CHECKOUT_REJECTED_EVENT_ROUTING_KEY
            );
        }
    }

    private boolean claim(UUID eventId, String eventType) {
        return jdbcTemplate.update("""
                INSERT INTO inbox_messages(event_id, event_type, processed_at)
                VALUES (?, ?, now())
                ON CONFLICT (event_id) DO NOTHING
                """, eventId, eventType) == 1;
    }

    private void publishResult(
            CheckoutCartCommand request,
            String outcome,
            String reason,
            String routingKey
    ) {
        CheckoutResult result = new CheckoutResult(
                UUID.randomUUID(),
                request.orderId(),
                request.cartId(),
                outcome,
                reason,
                Instant.now()
        );
        String eventType = "COMPLETED".equals(outcome)
                ? "CartCheckedOutEvent.v1"
                : "CartCheckoutRejectedEvent.v1";
        outboxService.enqueue(
                result.eventId(),
                eventType,
                routingKey,
                result
        );
    }

    public record CheckoutCartCommand(
            UUID eventId,
            UUID orderId,
            UUID cartId,
            Instant occurredAt
    ) {
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
