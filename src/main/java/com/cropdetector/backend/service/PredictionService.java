
package com.cropdetector.backend.service;

import java.util.List;
import java.util.UUID;

import com.cropdetector.backend.dto.PredictionDtos;
import com.cropdetector.backend.exception.ApiException;
import com.cropdetector.backend.model.Prediction;
import com.cropdetector.backend.repository.PredictionRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PredictionService {

    private final ImageValidationService imageValidator;
    private final MlClient mlClient;
    private final PredictionRepository predictions;
    private final double minimumConfidence;
    private final List<String> generalGuidance;

    public PredictionService(
        ImageValidationService imageValidator,
        MlClient mlClient,
        PredictionRepository predictions,
        @Value("${app.prediction.minimum-confidence}") double minimumConfidence,
        @Value("${app.guidance.general}") String guidanceConfig
    ) {
        this.imageValidator = imageValidator;
        this.mlClient = mlClient;
        this.predictions = predictions;
        this.minimumConfidence = minimumConfidence;
        this.generalGuidance = List.of(guidanceConfig.split("\\|"));
    }

    @Transactional
    public PredictionDtos.PredictionResponse predict(
        UUID userId, MultipartFile image, String crop
    ) {
        var validImage = imageValidator.validate(image);
        var result = mlClient.predict(validImage, crop);

        boolean uncertain = result.lowConfidence() || result.confidence() < minimumConfidence;
        String status = !result.isLeaf() ? "NOT_A_LEAF" : (uncertain ? "UNCERTAIN" : "COMPLETED");

        java.util.List<String> guidance = new java.util.ArrayList<>();
        if (result.message() != null && !result.message().isBlank()) {
            guidance.add(result.message());
        }

        if (uncertain) {
            guidance.add("The result is uncertain. Try capturing a clearer, well-lit photo of a single leaf flat against a neutral background.");
            guidance.add("Ask a local agricultural expert to confirm the symptoms.");
        } else {
            guidance.addAll(generalGuidance);
        }

        if (result.disclaimer() != null && !result.disclaimer().isBlank()) {
            guidance.add(result.disclaimer());
        }

        // Resolve crop: prefer user input if provided, otherwise fallback to the ML detected crop
        String resolvedCrop = (crop != null && !crop.isBlank())
            ? crop.trim()
            : (result.crop() != null && !result.crop().isBlank() ? result.crop().trim() : null);

        // Fallback: if still null, try extracting crop name from the predictedClass label (e.g. "Potato___Late_blight" -> "Potato")
        if ((resolvedCrop == null || resolvedCrop.isBlank()) && result.predictedClass() != null && result.predictedClass().contains("___")) {
            resolvedCrop = result.predictedClass().split("___")[0].trim();
        }

        Prediction prediction = new Prediction(
            userId,
            resolvedCrop,
            result.predictedClass(),
            result.confidence(),
            status,
            result.modelVersion(),
            guidance,
            true
        );

        return toResponse(predictions.save(prediction));
    }

    @Transactional(readOnly = true)
    public Page<PredictionDtos.PredictionResponse> history(
        UUID userId, int page, int size
    ) {
        if (page < 0 || size < 1 || size > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "INVALID_PAGINATION",
                "Page must be non-negative and size must be between 1 and 50.");
        }

        return predictions.findByUserId(
            userId, PageRequest.of(page, size))
            .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PredictionDtos.PredictionResponse getById(
        UUID predictionId, UUID userId
    ) {
        Prediction prediction = predictions
            .findByIdAndUserId(predictionId, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "PREDICTION_NOT_FOUND", "Prediction not found."));

        return toResponse(prediction);
    }

    @Transactional
    public void delete(UUID predictionId, UUID userId) {
        Prediction prediction = predictions
            .findByIdAndUserId(predictionId, userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                "PREDICTION_NOT_FOUND", "Prediction not found."));

        predictions.delete(prediction);
    }

    private PredictionDtos.PredictionResponse toResponse(Prediction p) {
        return new PredictionDtos.PredictionResponse(
            p.getId().toString(),
            p.getCrop(),
            p.getPredictedClass(),
            p.getConfidence(),
            p.getStatus(),
            p.getModelVersion(),
            p.getGuidance(),
            p.isExpertConfirmationRecommended(),
            p.getCreatedAt()
        );
    }
}
