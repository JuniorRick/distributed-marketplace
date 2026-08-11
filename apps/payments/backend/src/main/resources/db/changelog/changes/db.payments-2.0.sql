--liquibase formatted sql

--changeset estinca:payments-003
--comment: support idempotent refunds and optimistic payment updates
ALTER TABLE payments
    ADD COLUMN refund_reference VARCHAR(100),
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
