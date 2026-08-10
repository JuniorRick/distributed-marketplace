package com.marketplace.payments.messaging;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class OutboxService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public OutboxService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void enqueue(UUID eventId, String eventType, String routingKey, Object event) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO event_outbox(
                        event_id, event_type, routing_key, payload, occurred_at
                    ) VALUES (?, ?, ?, ?, now())
                    """, eventId, eventType, routingKey, objectMapper.writeValueAsString(event));
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Event could not be serialized", exception);
        }
    }
}
