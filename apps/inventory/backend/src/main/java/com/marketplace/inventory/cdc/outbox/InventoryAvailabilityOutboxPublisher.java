package com.marketplace.inventory.cdc.outbox;

import static com.marketplace.inventory.cdc.InventoryAvailabilityKafkaConfiguration.INVENTORY_AVAILABILITY_TOPIC;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InventoryAvailabilityOutboxPublisher {

    private final JdbcTemplate jdbcTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public InventoryAvailabilityOutboxPublisher(
        JdbcTemplate jdbcTemplate,
        KafkaTemplate<String, String> kafkaTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(
        initialDelayString = "${marketplace.kafka.inventory-availability.outbox.initial-delay:1s}",
        fixedDelayString = "${marketplace.kafka.inventory-availability.outbox.fixed-delay:1s}"
    )
    @Transactional
    public void publishPending() {
        List<OutboxRecord> events = claimBatch();
        for (int index = 0; index < events.size(); index++) {
            OutboxRecord event = events.get(index);
            try {
                publish(event);
                jdbcTemplate.update(
                    """
                        UPDATE inventory_availability_outbox
                        SET published_at = now(), locked_at = NULL, last_error = NULL
                        WHERE event_id = ?
                        """, event.eventId()
                );
            } catch (RuntimeException exception) {
                jdbcTemplate.update(
                    """
                        UPDATE inventory_availability_outbox
                        SET attempts = attempts + 1, locked_at = NULL, last_error = ?
                        WHERE event_id = ?
                        """, abbreviate(exception.getMessage()), event.eventId()
                );
                unlock(events.subList(index + 1, events.size()));
                break;
            }
        }
    }

    private void publish(OutboxRecord event) {
        try {
            kafkaTemplate.send(INVENTORY_AVAILABILITY_TOPIC, event.productId().toString(), event.payload())
                .get(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Kafka", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Kafka did not confirm inventory availability", exception);
        }
    }

    private List<OutboxRecord> claimBatch() {
        return jdbcTemplate.query(
            """
                WITH claimed AS (
                    SELECT event_id
                    FROM inventory_availability_outbox
                    WHERE published_at IS NULL
                      AND (locked_at IS NULL OR locked_at < now() - interval '30 seconds')
                    ORDER BY occurred_at, event_id
                    FOR UPDATE SKIP LOCKED
                    LIMIT 100
                ), updated AS (
                    UPDATE inventory_availability_outbox AS outbox
                    SET locked_at = now()
                    FROM claimed
                    WHERE outbox.event_id = claimed.event_id
                    RETURNING outbox.event_id, outbox.product_id, outbox.payload, outbox.occurred_at
                )
                SELECT event_id, product_id, payload
                FROM updated
                ORDER BY occurred_at, event_id
                """, (resultSet, rowNumber) -> new OutboxRecord(
                resultSet.getObject("event_id", UUID.class),
                resultSet.getObject("product_id", UUID.class),
                resultSet.getString("payload")
            )
        );
    }

    private void unlock(List<OutboxRecord> events) {
        events.forEach(event -> jdbcTemplate.update(
            """
                UPDATE inventory_availability_outbox
                SET locked_at = NULL
                WHERE event_id = ?
                """, event.eventId()
        ));
    }

    private static String abbreviate(String message) {
        if (message == null) {
            return "Unknown Kafka error";
        }
        return message.substring(0, Math.min(message.length(), 500));
    }

    private record OutboxRecord(UUID eventId, UUID productId, String payload) {

    }
}
