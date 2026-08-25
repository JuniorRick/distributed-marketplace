package com.marketplace.inventory.cdc.outbox;

import static com.marketplace.inventory.cdc.InventoryAvailabilityKafkaConfiguration.INVENTORY_AVAILABILITY_TOPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketplace.inventory.shared.InventoryIntegrationTest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class InventoryAvailabilityOutboxPublisherIT extends InventoryIntegrationTest {

    @Autowired
    private InventoryAvailabilityOutboxPublisher publisher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void publishesClaimedEventsInOccurrenceOrder() {
        UUID productId = UUID.randomUUID();
        UUID newestId = UUID.randomUUID();
        UUID oldestId = UUID.randomUUID();
        UUID middleId = UUID.randomUUID();
        insert(newestId, productId, "newest", Instant.parse("2026-01-03T00:00:00Z"));
        insert(oldestId, productId, "oldest", Instant.parse("2026-01-01T00:00:00Z"));
        insert(middleId, productId, "middle", Instant.parse("2026-01-02T00:00:00Z"));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishPending();

        ArgumentCaptor<String> payloads = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, atLeast(3)).send(
                eq(INVENTORY_AVAILABILITY_TOPIC),
                anyString(),
                payloads.capture()
        );
        List<String> testPayloads = payloads.getAllValues().stream()
                .filter(payload -> List.of("oldest", "middle", "newest").contains(payload))
                .toList();
        assertThat(testPayloads).containsExactly("oldest", "middle", "newest");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM inventory_availability_outbox
                WHERE event_id IN (?, ?, ?)
                  AND published_at IS NOT NULL
                """, Integer.class, oldestId, middleId, newestId)).isEqualTo(3);
    }

    private void insert(UUID eventId, UUID productId, String payload, Instant occurredAt) {
        jdbcTemplate.update("""
                INSERT INTO inventory_availability_outbox(
                    event_id, product_id, payload, occurred_at
                ) VALUES (?, ?, ?, ?)
                """, eventId, productId, payload, Timestamp.from(occurredAt));
    }
}
