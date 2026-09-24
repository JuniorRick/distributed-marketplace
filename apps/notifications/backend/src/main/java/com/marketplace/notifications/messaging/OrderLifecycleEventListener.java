package com.marketplace.notifications.messaging;

import com.marketplace.notifications.notification.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class OrderLifecycleEventListener {
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    public OrderLifecycleEventListener(ObjectMapper objectMapper, NotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = OrderLifecycleMessagingConfiguration.QUEUE)
    public void handle(String payload) throws JacksonException {
        notificationService.record(objectMapper.readValue(payload, OrderLifecycleEvent.class));
    }
}
