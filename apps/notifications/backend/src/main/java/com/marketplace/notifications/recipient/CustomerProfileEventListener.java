package com.marketplace.notifications.recipient;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class CustomerProfileEventListener {
    private final ObjectMapper objectMapper;
    private final NotificationRecipientProjection projection;

    public CustomerProfileEventListener(ObjectMapper objectMapper, NotificationRecipientProjection projection) {
        this.objectMapper = objectMapper;
        this.projection = projection;
    }

    @RabbitListener(queues = CustomerMessagingConfiguration.QUEUE)
    public void handle(String payload, Message message) throws JacksonException {
        String eventType = message.getMessageProperties().getReceivedRoutingKey();
        projection.project(objectMapper.readValue(payload, CustomerProfileEvent.class), eventType);
    }
}
