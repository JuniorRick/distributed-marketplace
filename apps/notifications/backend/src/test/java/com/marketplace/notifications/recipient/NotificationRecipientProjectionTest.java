package com.marketplace.notifications.recipient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class NotificationRecipientProjectionTest {
    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void projectsAClaimedCustomerEvent() {
        CustomerProfileEvent event = event();
        when(jdbcTemplate.update(startsWith("INSERT INTO inbox_messages"), eq(event.eventId()), any()))
            .thenReturn(1);

        new NotificationRecipientProjection(jdbcTemplate).project(event, "CustomerCreatedEvent.v1");

        verify(jdbcTemplate).update(
            startsWith("INSERT INTO notification_recipients"),
            eq(event.customerId()), eq(event.email()), eq(event.phone()), eq(true), eq(false), eq(3L)
        );
    }

    @Test
    void ignoresDuplicateEvent() {
        CustomerProfileEvent event = event();
        when(jdbcTemplate.update(startsWith("INSERT INTO inbox_messages"), eq(event.eventId()), any()))
            .thenReturn(0);

        new NotificationRecipientProjection(jdbcTemplate).project(event, "CustomerCreatedEvent.v1");

        verify(jdbcTemplate, never()).update(startsWith("INSERT INTO notification_recipients"), any(Object[].class));
    }

    private static CustomerProfileEvent event() {
        return new CustomerProfileEvent(
            UUID.randomUUID(), UUID.randomUUID(), "user@example.com", "+40123456789", true, false, 3, Instant.now()
        );
    }
}
