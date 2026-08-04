--liquibase formatted sql

--changeset estinca:003
--comment: support asynchronous checkout state and messaging
ALTER TABLE orders ADD COLUMN failure_reason VARCHAR(500);

CREATE TABLE event_outbox (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    routing_key VARCHAR(150) NOT NULL,
    payload TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    locked_at TIMESTAMPTZ,
    attempts INT NOT NULL DEFAULT 0,
    last_error VARCHAR(500)
);
CREATE INDEX idx_orders_outbox_pending
    ON event_outbox(occurred_at)
    WHERE published_at IS NULL;

CREATE TABLE inbox_messages (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);
