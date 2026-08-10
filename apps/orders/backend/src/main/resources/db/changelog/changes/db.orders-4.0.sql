--liquibase formatted sql

--changeset estinca:005
ALTER TABLE orders ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
