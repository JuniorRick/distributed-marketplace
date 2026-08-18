package com.marketplace.catalog.inventory.availability.cdc;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration(proxyBeanMethods = false)
public class InventoryAvailabilityKafkaConfiguration {

    public static final String INVENTORY_AVAILABILITY_TOPIC = "inventory-availability-topic";

    @Bean
    NewTopic inventoryAvailabilityTopic(
            @Value("${marketplace.kafka.inventory-availability.partitions:6}") int partitions,
            @Value("${marketplace.kafka.inventory-availability.replication-factor:1}") short replicationFactor
    ) {
        return TopicBuilder.name(INVENTORY_AVAILABILITY_TOPIC)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }
}
