package com.cropdetector.backend.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "user_preferences",
    indexes = @Index(name = "idx_user_preferences_user_id", columnList = "user_id", unique = true))
public class UserPreferences {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false, length = 10)
    private String language = "en";

    @Column(name = "notifications_enabled", nullable = false)
    private boolean notificationsEnabled = true;

    @Column(name = "email_updates_enabled", nullable = false)
    private boolean emailUpdatesEnabled = false;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserPreferences() {}

    public UserPreferences(UUID userId) {
        this.userId = userId;
        this.language = "en";
        this.notificationsEnabled = true;
        this.emailUpdatesEnabled = false;
        this.updatedAt = Instant.now();
    }

    @PrePersist
    @PreUpdate
    void onSave() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }
    public boolean isEmailUpdatesEnabled() { return emailUpdatesEnabled; }
    public void setEmailUpdatesEnabled(boolean emailUpdatesEnabled) { this.emailUpdatesEnabled = emailUpdatesEnabled; }
    public Instant getUpdatedAt() { return updatedAt; }
}
