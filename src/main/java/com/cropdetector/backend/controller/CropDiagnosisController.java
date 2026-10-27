package com.cropdetector.backend.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.cropdetector.backend.dto.CropPredictionResponse;
import com.cropdetector.backend.model.AppUser;
import com.cropdetector.backend.model.Prediction;
import com.cropdetector.backend.repository.PredictionRepository;
import com.cropdetector.backend.service.MlServiceClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/crops", "/api/v1/crops"})
@CrossOrigin(origins = "*")
public class CropDiagnosisController {

    private static final Logger log = LoggerFactory.getLogger(CropDiagnosisController.class);

    private final MlServiceClient mlServiceClient;
    private final PredictionRepository predictionRepository;

    public CropDiagnosisController(
            MlServiceClient mlServiceClient,
            PredictionRepository predictionRepository) {
        this.mlServiceClient = mlServiceClient;
        this.predictionRepository = predictionRepository;
    }

    /**
     * Public endpoint for frontend / mobile app to upload leaf photo.
     * POST /api/crops/diagnose
     * POST /api/v1/crops/diagnose
     * Automatically saves diagnosis history into database.
     */
    @PostMapping(value = "/diagnose", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CropPredictionResponse> diagnoseLeaf(
            @AuthenticationPrincipal AppUser user,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "crop", required = false) String crop) throws IOException {

        MultipartFile targetImage = (image != null && !image.isEmpty()) ? image : file;
        if (targetImage == null || targetImage.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        CropPredictionResponse result = mlServiceClient.predictDisease(targetImage, crop);

        // Persist diagnosis in database
        try {
            UUID userId = user != null ? user.getId() : null;
            String cropName = (crop != null && !crop.isBlank())
                    ? crop.trim()
                    : (result.getPrediction() != null && result.getPrediction().getCrop() != null && !result.getPrediction().getCrop().isBlank()
                        ? result.getPrediction().getCrop().trim()
                        : null);

            String label = (result.getPrediction() != null && result.getPrediction().getLabel() != null)
                    ? result.getPrediction().getLabel().trim()
                    : "Unknown";

            if ((cropName == null || cropName.isBlank()) && label.contains("___")) {
                cropName = label.split("___")[0].trim();
            }

            double confidence = result.getPrediction() != null ? result.getPrediction().getConfidence() : 0.0;
            String status = result.isLowConfidence() ? "UNCERTAIN" : (result.isLeaf() ? "COMPLETED" : "NOT_A_LEAF");

            List<String> guidance = new ArrayList<>();
            if (result.getMessage() != null && !result.getMessage().isBlank()) {
                guidance.add(result.getMessage());
            }
            if (result.getDisclaimer() != null && !result.getDisclaimer().isBlank()) {
                guidance.add(result.getDisclaimer());
            }

            Prediction prediction = new Prediction(
                    userId,
                    cropName,
                    label,
                    confidence,
                    status,
                    result.getModelVersion(),
                    guidance,
                    true
            );
            predictionRepository.save(prediction);
            log.info("Saved diagnosis prediction for crop={}, disease={} to database (id={})",
                    cropName, label, prediction.getId());
        } catch (Exception ex) {
            log.error("Failed to persist prediction in database: {}", ex.getMessage());
        }

        return ResponseEntity.ok(result);
    }
}
