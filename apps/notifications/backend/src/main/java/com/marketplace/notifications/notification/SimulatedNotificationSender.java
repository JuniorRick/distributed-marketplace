package com.marketplace.notifications.notification;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SimulatedNotificationSender implements NotificationSender {
    private static final Logger LOG = LoggerFactory.getLogger(SimulatedNotificationSender.class);

    @Override
    public void send(UUID notificationId, UUID customerId, String email, String message) {
        LOG.info(
            "Simulated email delivery notificationId={} customerId={} email={} message={}",
            notificationId,
            customerId,
            email,
            message
        );
    }
}
