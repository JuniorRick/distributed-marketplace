package com.marketplace.notifications.notification;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificationDispatcher {
    private final JdbcTemplate jdbcTemplate;
    private final NotificationSender notificationSender;
    private final int maxAttempts;
    private final Duration retryDelay;

    public NotificationDispatcher(
        JdbcTemplate jdbcTemplate,
        NotificationSender notificationSender,
        @Value("${marketplace.notifications.max-attempts:5}") int maxAttempts,
        @Value("${marketplace.notifications.retry-delay:10s}") Duration retryDelay
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.notificationSender = notificationSender;
        this.maxAttempts = maxAttempts;
        this.retryDelay = retryDelay;
    }

    @Scheduled(
        initialDelayString = "${marketplace.notifications.initial-delay:1s}",
        fixedDelayString = "${marketplace.notifications.fixed-delay:1s}"
    )
    @Transactional
    public void sendPending() {
        skipDisabledRecipients();
        for (PendingNotification notification : claimBatch()) {
            try {
                notificationSender.send(
                    notification.notificationId(),
                    notification.customerId(),
                    notification.email(),
                    notification.message()
                );
                jdbcTemplate.update(
                    """
                        UPDATE notifications
                        SET status = 'SENT', sent_at = now(), locked_at = NULL, last_error = NULL
                        WHERE public_id = ?
                        """,
                    notification.notificationId()
                );
            } catch (RuntimeException exception) {
                jdbcTemplate.update(
                    """
                        UPDATE notifications
                        SET status = 'FAILED', attempts = attempts + 1, next_attempt_at = ?,
                            locked_at = NULL, last_error = ?
                        WHERE public_id = ?
                        """,
                    Instant.now().plus(retryDelay),
                    abbreviate(exception.getMessage()),
                    notification.notificationId()
                );
            }
        }
    }

    private void skipDisabledRecipients() {
        jdbcTemplate.update(
            """
                UPDATE notifications AS notification
                SET status = 'SKIPPED', locked_at = NULL,
                    last_error = 'Email notifications disabled by customer preference'
                FROM notification_recipients AS recipient
                WHERE notification.customer_id = recipient.customer_id
                  AND notification.status IN ('PENDING', 'FAILED')
                  AND recipient.email_enabled = false
                """
        );
    }

    private List<PendingNotification> claimBatch() {
        return jdbcTemplate.query(
            """
                WITH claimed AS (
                    SELECT notification.id
                    FROM notifications AS notification
                    JOIN notification_recipients AS recipient
                      ON recipient.customer_id = notification.customer_id
                    WHERE notification.status IN ('PENDING', 'FAILED')
                      AND recipient.email_enabled = true
                      AND recipient.email IS NOT NULL
                      AND notification.attempts < ?
                      AND notification.next_attempt_at <= now()
                      AND (notification.locked_at IS NULL OR notification.locked_at < now() - interval '30 seconds')
                    ORDER BY notification.created_at, notification.id
                    FOR UPDATE OF notification SKIP LOCKED
                    LIMIT 100
                ), updated AS (
                    UPDATE notifications AS notification
                    SET locked_at = now()
                    FROM claimed
                    WHERE notification.id = claimed.id
                    RETURNING notification.public_id, notification.customer_id,
                              notification.message, notification.created_at, notification.id
                )
                SELECT updated.public_id, updated.customer_id, recipient.email, updated.message
                FROM updated
                JOIN notification_recipients AS recipient ON recipient.customer_id = updated.customer_id
                ORDER BY created_at, id
                """,
            (resultSet, rowNumber) -> new PendingNotification(
                resultSet.getObject("public_id", UUID.class),
                resultSet.getObject("customer_id", UUID.class),
                resultSet.getString("email"),
                resultSet.getString("message")
            ),
            maxAttempts
        );
    }

    private static String abbreviate(String message) {
        if (message == null) {
            return "Unknown notification delivery error";
        }
        return message.substring(0, Math.min(message.length(), 500));
    }

    private record PendingNotification(UUID notificationId, UUID customerId, String email, String message) {
    }
}
