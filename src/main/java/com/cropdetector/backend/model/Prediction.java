package com.cropdetector.backend.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "predictions",
    indexes = {
        @Index(name = "idx_predictions_user_id", columnList = "user_id"),
        @Index(name = "idx_predictions_created_at", columnList = "created_at")
    })
public class Prediction {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(length = 100)
    private String crop;

    @Column(name = "predicted_class", nullable = false, length = 150)
    private String predictedClass;

    @Column(nullable = false)
    private double confidence;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "prediction_guidance",
        joinColumns = @JoinColumn(name = "prediction_id"))
    @Column(name = "guidance_item", length = 1000)
    private List<String> guidance = new ArrayList<>();

    @Column(name = "expert_confirmation_recommended", nullable = false)
    private boolean expertConfirmationRecommended = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Prediction() {}

    public Prediction(
        UUID userId,
        String crop,
        String predictedClass,
        double confidence,
        String status,
        String modelVersion,
        List<String> guidance,
        boolean expertConfirmationRecommended
    ) {
        this.userId = userId;
        this.crop = crop;
        this.predictedClass = predictedClass;
        this.confidence = confidence;
        this.status = status;
        this.modelVersion = modelVersion;
        this.guidance = guidance != null ? new ArrayList<>(guidance) : new ArrayList<>();
        this.expertConfirmationRecommended = expertConfirmationRecommended;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getCrop() { return crop; }
    public String getPredictedClass() { return predictedClass; }
    public double getConfidence() { return confidence; }
    public String getStatus() { return status; }
    public String getModelVersion() { return modelVersion; }
    public List<String> getGuidance() { return guidance; }
    public boolean isExpertConfirmationRecommended() { return expertConfirmationRecommended; }
    public Instant getCreatedAt() { return createdAt; }

    public void setUserId(UUID userId) { this.userId = userId; }
    public void setCrop(String crop) { this.crop = crop; }
    public void setPredictedClass(String predictedClass) { this.predictedClass = predictedClass; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public void setStatus(String status) { this.status = status; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public void setGuidance(List<String> guidance) { this.guidance = guidance; }
    public void setExpertConfirmationRecommended(boolean expertConfirmationRecommended) {
        this.expertConfirmationRecommended = expertConfirmationRecommended;
    }
}
