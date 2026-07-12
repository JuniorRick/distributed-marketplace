--liquibase formatted sql

--changeset estinca:001
--comment: create carts table
CREATE TABLE IF NOT EXISTS carts (
     id BIGINT PRIMARY KEY,
     public_id UUID NOT NULL UNIQUE,
     customer_id BIGINT,
     status VARCHAR(32) NOT NULL,
     created_at TIMESTAMPTZ NOT NULL,
     updated_at TIMESTAMPTZ NOT NULL
);

--changeset estinca:002
--comment: create cart_items table
CREATE TABLE IF NOT EXISTS cart_items (
     id BIGINT PRIMARY KEY,
     public_id UUID NOT NULL UNIQUE,
     cart_id BIGINT NOT NULL,
     product_id BIGINT NOT NULL,
     product_sku VARCHAR(64) NOT NULL,
     product_name VARCHAR(160) NOT NULL,
     unit_price_amount NUMERIC(12, 2) NOT NULL,
     currency VARCHAR(3) NOT NULL,
     quantity INT NOT NULL,
     created_at TIMESTAMPTZ NOT NULL,
     updated_at TIMESTAMPTZ NOT NULL,

     constraint fk__cart_id foreign key (cart_id) references carts(id)
);

