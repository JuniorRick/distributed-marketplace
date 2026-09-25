--liquibase formatted sql

--changeset estinca:notifications-003
--comment: add local customer notification recipient projection
CREATE TABLE notification_recipients (
    customer_id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    phone VARCHAR(32),
    email_enabled BOOLEAN NOT NULL,
    sms_enabled BOOLEAN NOT NULL,
    source_version BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
