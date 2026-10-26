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
        String modelVersion
    ) {}

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

        return new MlResponse(predictedClass, confidence, modelVersion);
    }
}
