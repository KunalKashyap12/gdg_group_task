package com.cropdetector.backend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CropPredictionResponse {

    private String status;

    @JsonProperty("is_leaf")
    private boolean isLeaf;

    private PredictionItem prediction;

    @JsonProperty("top_k")
    private List<TopKItem> topK;

    @JsonProperty("low_confidence")
    private boolean lowConfidence;

    private double threshold;
    private String message;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("inference_time_ms")
    private double inferenceTimeMs;

    private String disclaimer;

    public CropPredictionResponse() {}

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void setLeaf(boolean leaf) {
        isLeaf = leaf;
    }

    public PredictionItem getPrediction() {
        return prediction;
    }

    public void setPrediction(PredictionItem prediction) {
        this.prediction = prediction;
    }

    public List<TopKItem> getTopK() {
        return topK;
    }

    public void setTopK(List<TopKItem> topK) {
        this.topK = topK;
    }

    public boolean isLowConfidence() {
        return lowConfidence;
    }

    public void setLowConfidence(boolean lowConfidence) {
        this.lowConfidence = lowConfidence;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public double getInferenceTimeMs() {
        return inferenceTimeMs;
    }

    public void setInferenceTimeMs(double inferenceTimeMs) {
        this.inferenceTimeMs = inferenceTimeMs;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    @Override
    public String toString() {
        return "CropPredictionResponse{" +
                "status='" + status + '\'' +
                ", isLeaf=" + isLeaf +
                ", prediction=" + prediction +
                ", topK=" + topK +
                ", lowConfidence=" + lowConfidence +
                ", threshold=" + threshold +
                ", message='" + message + '\'' +
                ", modelVersion='" + modelVersion + '\'' +
                ", inferenceTimeMs=" + inferenceTimeMs +
                ", disclaimer='" + disclaimer + '\'' +
                '}';
    }
}
