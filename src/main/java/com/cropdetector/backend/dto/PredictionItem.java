package com.cropdetector.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PredictionItem {

    @JsonProperty("class_id")
    private int classId;

    private String crop;
    private String disease;
    private String label;
    private double confidence;

    public PredictionItem() {}

    public PredictionItem(int classId, String crop, String disease, String label, double confidence) {
        this.classId = classId;
        this.crop = crop;
        this.disease = disease;
        this.label = label;
        this.confidence = confidence;
    }

    public int getClassId() {
        return classId;
    }

    public void setClassId(int classId) {
        this.classId = classId;
    }

    public String getCrop() {
        return crop;
    }

    public void setCrop(String crop) {
        this.crop = crop;
    }

    public String getDisease() {
        return disease;
    }

    public void setDisease(String disease) {
        this.disease = disease;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    @Override
    public String toString() {
        return "PredictionItem{" +
                "classId=" + classId +
                ", crop='" + crop + '\'' +
                ", disease='" + disease + '\'' +
                ", label='" + label + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}
