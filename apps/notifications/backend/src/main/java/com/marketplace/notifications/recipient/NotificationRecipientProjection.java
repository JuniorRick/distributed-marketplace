package com.marketplace.notifications.recipient;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationRecipientProjection {
    private final JdbcTemplate jdbcTemplate;

    public NotificationRecipientProjection(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void project(CustomerProfileEvent event, String eventType) {
        if (!claim(event, eventType)) {
            return;
        }
        jdbcTemplate.update(
            """
                INSERT INTO notification_recipients(
                    customer_id, email, phone, email_enabled, sms_enabled, source_version, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, now())
                ON CONFLICT (customer_id) DO UPDATE
                SET email = EXCLUDED.email,
                    phone = EXCLUDED.phone,
                    email_enabled = EXCLUDED.email_enabled,
                    sms_enabled = EXCLUDED.sms_enabled,
                    source_version = EXCLUDED.source_version,
                    updated_at = EXCLUDED.updated_at
                WHERE notification_recipients.source_version < EXCLUDED.source_version
                """,
            event.customerId(),
            event.email(),
            event.phone(),
            event.emailNotificationsEnabled(),
            event.smsNotificationsEnabled(),
            event.version()
        );
    }

    private boolean claim(CustomerProfileEvent event, String eventType) {
        return jdbcTemplate.update(
            """
                INSERT INTO inbox_messages(event_id, event_type, processed_at)
                VALUES (?, ?, now())
                ON CONFLICT (event_id) DO NOTHING
                """,
            event.eventId(), eventType
        ) == 1;
    }
}
