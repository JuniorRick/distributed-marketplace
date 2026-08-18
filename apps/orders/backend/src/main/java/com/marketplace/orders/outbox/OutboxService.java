package com.marketplace.orders.outbox;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class OutboxService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public OutboxService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void enqueue(
            UUID eventId,
            String eventType,
            String exchangeName,
            String routingKey,
            Object event
    ) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO event_outbox(
                        event_id, event_type, exchange_name, routing_key, payload, occurred_at
                    ) VALUES (?, ?, ?, ?, ?, now())
                    """, eventId, eventType, exchangeName, routingKey,
                    objectMapper.writeValueAsString(event));
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Event could not be serialized", exception);
        }
    }
}
