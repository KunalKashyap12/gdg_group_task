package com.cropdetector.backend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TopKItem {

    private String label;
    private double confidence;

    public TopKItem() {}

    public TopKItem(String label, double confidence) {
        this.label = label;
        this.confidence = confidence;
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
        return "TopKItem{" +
                "label='" + label + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}
