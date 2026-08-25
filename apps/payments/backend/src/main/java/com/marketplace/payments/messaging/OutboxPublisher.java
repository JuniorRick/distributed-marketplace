package com.marketplace.payments.messaging;

import com.marketplace.payments.payment.messaging.PaymentMessagingConfiguration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {

    private final JdbcTemplate jdbcTemplate;
    private final RabbitTemplate rabbitTemplate;

    public OutboxPublisher(JdbcTemplate jdbcTemplate, RabbitTemplate rabbitTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(
            initialDelayString = "${marketplace.outbox.initial-delay:1s}",
            fixedDelayString = "${marketplace.outbox.fixed-delay:1s}"
    )
    @Transactional
    public void publishPending() {
        for (OutboxRecord event : claimBatch()) {
            try {
                publish(event);
                jdbcTemplate.update("""
                        UPDATE event_outbox
                        SET published_at = now(), locked_at = NULL, last_error = NULL
                        WHERE event_id = ?
                        """, event.eventId());
            } catch (RuntimeException exception) {
                jdbcTemplate.update("""
                        UPDATE event_outbox
                        SET attempts = attempts + 1, locked_at = NULL, last_error = ?
                        WHERE event_id = ?
                        """, abbreviate(exception.getMessage()), event.eventId());
            }
        }
    }

    private void publish(OutboxRecord event) {
        CorrelationData correlation = new CorrelationData(event.eventId().toString());
        rabbitTemplate.convertAndSend(
                PaymentMessagingConfiguration.EXCHANGE,
                event.routingKey(),
                event.payload(),
                correlation
        );
        try {
            CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
            if (!confirm.ack()) {
                throw new IllegalStateException("Broker rejected event: " + confirm.reason());
            }
            if (correlation.getReturned() != null) {
                throw new IllegalStateException("Event had no matching queue");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for broker confirmation", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Broker did not confirm event", exception);
        }
    }

    private List<OutboxRecord> claimBatch() {
        return jdbcTemplate.query("""
                WITH claimed AS (
                    SELECT event_id
                    FROM event_outbox
                    WHERE published_at IS NULL
                      AND (locked_at IS NULL OR locked_at < now() - interval '30 seconds')
                    ORDER BY occurred_at, event_id
                    FOR UPDATE SKIP LOCKED
                    LIMIT 100
                ), updated AS (
                    UPDATE event_outbox AS outbox
                    SET locked_at = now()
                    FROM claimed
                    WHERE outbox.event_id = claimed.event_id
                    RETURNING outbox.event_id, outbox.routing_key, outbox.payload, outbox.occurred_at
                )
                SELECT event_id, routing_key, payload
                FROM updated
                ORDER BY occurred_at, event_id
                """, (resultSet, rowNumber) -> new OutboxRecord(
                resultSet.getObject("event_id", UUID.class),
                resultSet.getString("routing_key"),
                resultSet.getString("payload")
        ));
    }

    private static String abbreviate(String message) {
        if (message == null) {
            return "Unknown broker error";
        }
        return message.substring(0, Math.min(message.length(), 500));
    }

    private record OutboxRecord(UUID eventId, String routingKey, String payload) {
    }
}
