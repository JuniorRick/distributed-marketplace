--liquibase formatted sql

--changeset estinca:006
--comment: track bounded saga reconciliation attempts
ALTER TABLE orders
    ADD COLUMN reconciliation_attempts INT NOT NULL DEFAULT 0,
    ALTER COLUMN status TYPE VARCHAR(48);

CREATE INDEX idx_orders_reconciliation
    ON orders(status, updated_at)
    WHERE status IN (
        'CHECKOUT_PENDING',
        'INVENTORY_RESERVATION_PENDING',
        'PAYMENT_PENDING',
        'INVENTORY_COMMIT_PENDING',
        'INVENTORY_RELEASE_PENDING',
        'INVENTORY_RELEASE_FOR_REFUND_PENDING',
        'REFUND_PENDING'
    );
