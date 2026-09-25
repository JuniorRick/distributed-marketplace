package com.marketplace.customers.customer.messaging;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class CustomerMessagingConfiguration {
    public static final String EXCHANGE = "marketplace.customers.v1";
    public static final String CUSTOMER_CREATED_EVENT_ROUTING_KEY = "customer.event.created.v1";
    public static final String CUSTOMER_CONTACT_UPDATED_EVENT_ROUTING_KEY = "customer.event.contact-updated.v1";
    public static final String CUSTOMER_PREFERENCES_UPDATED_EVENT_ROUTING_KEY = "customer.event.notification-preferences-updated.v1";

    @Bean
    DirectExchange customerExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }
}
