package com.marketplace.inventory.shared;

import org.testcontainers.postgresql.PostgreSQLContainer;

public final class SharedPostgresContainer {

    public static final PostgreSQLContainer INSTANCE =
        new PostgreSQLContainer("postgres:17-alpine")
            .withInitScript("db/test/create-inventory-schema.sql");

    static {
        INSTANCE.start();
    }

    private SharedPostgresContainer() {
    }
}