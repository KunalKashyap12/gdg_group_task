package com.cropdetector.backend.service;

import com.cropdetector.backend.dto.CropPredictionResponse;
import com.cropdetector.backend.dto.PredictionItem;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MlClient {

    private static final Logger log = LoggerFactory.getLogger(MlClient.class);

    private final MlServiceClient mlServiceClient;

    public MlClient(MlServiceClient mlServiceClient) {
        this.mlServiceClient = mlServiceClient;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MlResponse(
        String crop,
        String disease,
        String predictedClass,
        double confidence,
        String modelVersion,
        String message,
        String disclaimer,
        boolean lowConfidence,
        boolean isLeaf
    ) {
        // Backward-compatible constructors
        public MlResponse(String predictedClass, double confidence, String modelVersion) {
            this(null, null, predictedClass, confidence, modelVersion, null, null, false, true);
        }

        public MlResponse(String predictedClass, double confidence, String modelVersion,
                          String message, String disclaimer, boolean lowConfidence, boolean isLeaf) {
            this(null, null, predictedClass, confidence, modelVersion, message, disclaimer, lowConfidence, isLeaf);
        }
    }

    public MlResponse predict(
        ImageValidationService.ValidatedImage image, String crop
    ) {
        CropPredictionResponse response = mlServiceClient.predictDisease(
            image.bytes(),
            "image/png".equalsIgnoreCase(image.contentType()) ? "leaf.png" : "leaf.jpg",
            image.contentType(),
            crop
        );

        PredictionItem item = response.getPrediction();
        String cropName = (item != null && item.getCrop() != null && !item.getCrop().isBlank())
                ? item.getCrop().trim()
                : null;

        String diseaseName = (item != null && item.getDisease() != null && !item.getDisease().isBlank())
                ? item.getDisease().trim()
                : null;

        String label = (item != null && item.getLabel() != null && !item.getLabel().isBlank())
                ? item.getLabel().trim()
                : null;

        if (label != null && label.contains("___")) {
            String[] parts = label.split("___", 2);
            if (cropName == null || cropName.isBlank()) {
                cropName = parts[0].trim();
            }
            if (diseaseName == null || diseaseName.isBlank()) {
                diseaseName = parts[1].replace('_', ' ').trim();
            }
        }

        String predictedClass = (label != null)
                ? label
                : (diseaseName != null ? diseaseName : "Healthy");

        double confidence = (item != null) ? item.getConfidence() : 0.0;
        String modelVersion = response.getModelVersion() != null ? response.getModelVersion() : "1.0.0";
        String message = response.getMessage();
        String disclaimer = response.getDisclaimer();
        boolean lowConfidence = response.isLowConfidence();
        boolean isLeaf = response.isLeaf();

        return new MlResponse(
                cropName,
                diseaseName,
                predictedClass,
                confidence,
                modelVersion,
                message,
                disclaimer,
                lowConfidence,
                isLeaf
        );
    }
}
