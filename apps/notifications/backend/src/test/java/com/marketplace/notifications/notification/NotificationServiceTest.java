package com.marketplace.notifications.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.marketplace.notifications.messaging.OrderLifecycleEvent;
import com.marketplace.notifications.notification.repository.Notification;
import com.marketplace.notifications.notification.repository.NotificationRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void recordsConfirmedOrderNotification() {
        OrderLifecycleEvent event = event("CONFIRMED", null);
        when(jdbcTemplate.update(
            anyString(),
            eq(event.eventId()),
            eq("OrderConfirmedEvent.v1")
        )).thenReturn(1);

        notificationService.record(event);

        ArgumentCaptor<Notification> notification = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notification.capture());
        assertThat(notification.getValue().getSourceEventId()).isEqualTo(event.eventId());
        assertThat(notification.getValue().getOrderId()).isEqualTo(event.orderId());
        assertThat(notification.getValue().getCustomerId()).isEqualTo(event.customerId());
        assertThat(notification.getValue().getType()).isEqualTo(NotificationType.ORDER_CONFIRMED);
        assertThat(notification.getValue().getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(notification.getValue().getMessage()).contains("49.90 EUR");
    }

    @Test
    void ignoresDuplicateLifecycleEvent() {
        OrderLifecycleEvent event = event("REJECTED", "Payment was declined");
        when(jdbcTemplate.update(
            anyString(),
            eq(event.eventId()),
            eq("OrderRejectedEvent.v1")
        )).thenReturn(0);

        notificationService.record(event);

        verifyNoInteractions(notificationRepository);
    }

    private static OrderLifecycleEvent event(String status, String reason) {
        return new OrderLifecycleEvent(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            status,
            new BigDecimal("49.90"),
            "EUR",
            reason,
            Instant.now()
        );
    }
}
