package com.marketplace.customers.customer.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true)
    private UUID publicId;
    @Column(nullable = false, unique = true, length = 320)
    private String email;
    @Column(length = 32)
    private String phone;
    @Column(name = "email_notifications_enabled", nullable = false)
    private boolean emailNotificationsEnabled;
    @Column(name = "sms_notifications_enabled", nullable = false)
    private boolean smsNotificationsEnabled;
    @Column(name = "event_version", nullable = false)
    private long eventVersion;
    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Customer() {
    }

    private Customer(String email, String phone, boolean emailEnabled, boolean smsEnabled) {
        this.publicId = UUID.randomUUID();
        this.email = normalizeEmail(email);
        this.phone = normalizePhone(phone);
        this.emailNotificationsEnabled = emailEnabled;
        this.smsNotificationsEnabled = smsEnabled;
        this.eventVersion = 1;
    }

    public static Customer create(String email, String phone, boolean emailEnabled, boolean smsEnabled) {
        return new Customer(email, phone, emailEnabled, smsEnabled);
    }

    public void updateContact(String email, String phone) {
        this.email = normalizeEmail(email);
        this.phone = normalizePhone(phone);
        eventVersion++;
    }

    public void updateNotificationPreferences(boolean emailEnabled, boolean smsEnabled) {
        this.emailNotificationsEnabled = emailEnabled;
        this.smsNotificationsEnabled = smsEnabled;
        eventVersion++;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String normalizePhone(String phone) {
        return phone == null || phone.isBlank() ? null : phone.trim();
    }

    public UUID getPublicId() { return publicId; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public boolean isEmailNotificationsEnabled() { return emailNotificationsEnabled; }
    public boolean isSmsNotificationsEnabled() { return smsNotificationsEnabled; }
    public long getEventVersion() { return eventVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
