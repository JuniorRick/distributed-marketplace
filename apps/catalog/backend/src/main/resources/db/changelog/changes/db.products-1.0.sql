--liquibase formatted sql

--changeset estinca:001
--comment: create products table
CREATE TABLE IF NOT EXISTS products (
    id BIGINT PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    sku VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    price_amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx__products__status__name ON products(status, name);

--changeset estinca:002
--comment: seed-products
INSERT INTO products (id, public_id, sku, name, description, price_amount, currency, status, created_at, updated_at)
VALUES
    (1, '018f0cf1-38e5-7d1d-9f8d-7fce4fd67a01', 'MARKET-KEYBOARD-01', 'Mechanical Keyboard', 'Compact hot-swappable keyboard for focused work.', 129.00, 'USD', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, '018f0cf1-38e5-7d1d-9f8d-7fce4fd67a02', 'MARKET-MONITOR-01', '27 Inch Studio Monitor', 'Color-accurate display for design and engineering desks.', 349.00, 'USD', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, '018f0cf1-38e5-7d1d-9f8d-7fce4fd67a03', 'MARKET-DOCK-01', 'USB-C Dock', 'Desk dock with charging, HDMI, ethernet, and USB expansion.', 189.00, 'USD', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
