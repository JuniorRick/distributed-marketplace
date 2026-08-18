
--changeset estinca:003
--comment: add inventory availability replica table
create table inventory_availability (
    product_id UUID PRIMARY KEY NOT NULL,
    quantity   INT              NOT NULL
)