package com.cropdetector.backend.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "otp_verifications", indexes = {
    @Index(name = "idx_otp_email", columnList = "email"),
    @Index(name = "idx_otp_expires_at", columnList = "expires_at")
})
public class OtpVerification {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;

    @Column(length = 30)
    private String purpose = "LOGIN";

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OtpVerification() {}

    public OtpVerification(String email, String otpCode, String purpose, Instant expiresAt) {
        this.email = email;
        this.otpCode = otpCode;
        this.purpose = purpose != null ? purpose : "LOGIN";
        this.expiresAt = expiresAt;
        this.verified = false;
        this.attempts = 0;
        this.createdAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (purpose == null) {
            purpose = "LOGIN";
        }
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getOtpCode() { return otpCode; }
    public String getPurpose() { return purpose; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isVerified() { return verified; }
    public int getAttempts() { return attempts; }
    public Instant getCreatedAt() { return createdAt; }

    public void setVerified(boolean verified) { this.verified = verified; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public void incrementAttempts() { this.attempts++; }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
