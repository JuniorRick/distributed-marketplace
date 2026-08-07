--liquibase formatted sql

--changeset estinca:004
--comment: route outbox messages to their owning RabbitMQ exchange
ALTER TABLE event_outbox
    ADD COLUMN exchange_name VARCHAR(150) NOT NULL DEFAULT 'marketplace.checkout.v1';
