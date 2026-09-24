package com.marketplace.notifications.notification.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
