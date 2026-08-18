--liquibase formatted sql

--changeset estinca:inventory-005
--comment: add transactional Kafka outbox for inventory availability
CREATE TABLE inventory_availability_outbox (
    event_id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    payload TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    locked_at TIMESTAMPTZ,
    attempts INT NOT NULL DEFAULT 0,
    last_error VARCHAR(500)
);

CREATE INDEX idx_inventory_availability_outbox_pending
    ON inventory_availability_outbox(occurred_at)
    WHERE published_at IS NULL;
