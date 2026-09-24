package com.marketplace.notifications.notification;

import com.marketplace.notifications.messaging.OrderLifecycleEvent;
import com.marketplace.notifications.notification.repository.Notification;
import com.marketplace.notifications.notification.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final JdbcTemplate jdbcTemplate;
    private final NotificationRepository notificationRepository;

    public NotificationService(JdbcTemplate jdbcTemplate, NotificationRepository notificationRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void record(OrderLifecycleEvent event) {
        NotificationType type = notificationType(event.status());
        String eventType = switch (type) {
            case ORDER_CONFIRMED -> "OrderConfirmedEvent.v1";
            case ORDER_REJECTED -> "OrderRejectedEvent.v1";
            case ORDER_REFUNDED -> "OrderRefundedEvent.v1";
        };
        if (!claim(event.eventId(), eventType)) {
            return;
        }

        notificationRepository.save(new Notification(
            event.eventId(),
            event.orderId(),
            event.customerId(),
            type,
            message(event, type)
        ));
    }

    @Transactional(readOnly = true)
    public List<Notification> findByCustomerId(UUID customerId) {
        return notificationRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    private boolean claim(UUID eventId, String eventType) {
        return jdbcTemplate.update(
            """
                INSERT INTO inbox_messages(event_id, event_type, processed_at)
                VALUES (?, ?, now())
                ON CONFLICT (event_id) DO NOTHING
                """,
            eventId,
            eventType
        ) == 1;
    }

    private static NotificationType notificationType(String status) {
        return switch (status) {
            case "CONFIRMED" -> NotificationType.ORDER_CONFIRMED;
            case "REJECTED" -> NotificationType.ORDER_REJECTED;
            case "REFUNDED" -> NotificationType.ORDER_REFUNDED;
            default -> throw new IllegalArgumentException("Unsupported order notification status: " + status);
        };
    }

    private static String message(OrderLifecycleEvent event, NotificationType type) {
        return switch (type) {
            case ORDER_CONFIRMED -> "Order %s was confirmed. Total: %s %s"
                .formatted(event.orderId(), event.totalAmount(), event.currency());
            case ORDER_REJECTED -> "Order %s was rejected: %s"
                .formatted(event.orderId(), event.reason());
            case ORDER_REFUNDED -> "Order %s was refunded. Amount: %s %s"
                .formatted(event.orderId(), event.totalAmount(), event.currency());
        };
    }
}
