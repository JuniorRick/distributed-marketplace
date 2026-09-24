package com.marketplace.notifications.notification.api;

import com.marketplace.notifications.notification.NotificationService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/by-customer/{customerId}")
    List<NotificationResponse> getByCustomerId(@PathVariable UUID customerId) {
        return notificationService.findByCustomerId(customerId).stream()
            .map(NotificationResponse::from)
            .toList();
    }
}
