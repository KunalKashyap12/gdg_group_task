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
        String predictedClass,
        double confidence,
        String modelVersion,
        String message,
        String disclaimer,
        boolean lowConfidence,
        boolean isLeaf
    ) {
        // Backward-compatible constructor
        public MlResponse(String predictedClass, double confidence, String modelVersion) {
            this(predictedClass, confidence, modelVersion, null, null, false, true);
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
        String predictedClass = (item != null && item.getLabel() != null && !item.getLabel().isBlank())
                ? item.getLabel()
                : (item != null && item.getDisease() != null ? item.getDisease() : "Healthy");

        double confidence = (item != null) ? item.getConfidence() : 0.0;
        String modelVersion = response.getModelVersion() != null ? response.getModelVersion() : "1.0.0";
        String message = response.getMessage();
        String disclaimer = response.getDisclaimer();
        boolean lowConfidence = response.isLowConfidence();
        boolean isLeaf = response.isLeaf();

        return new MlResponse(
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
