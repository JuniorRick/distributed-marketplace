package com.marketplace.inventory.shared;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {
    "spring.datasource.hikari.schema=inventory",
    "spring.jpa.properties.hibernate.default_schema=inventory",
    "spring.rabbitmq.listener.simple.auto-startup=false",
    "spring.rabbitmq.dynamic=false"
})
public abstract class InventoryIntegrationTest {

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        var postgres = SharedPostgresContainer.INSTANCE;

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}