--liquibase formatted sql

--changeset estinca:inventory-004
--comment: add optimistic version to inventory reservations
ALTER TABLE inventory_reservations
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
