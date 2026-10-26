package com.cropdetector.backend.controller;

import java.io.IOException;

import com.cropdetector.backend.dto.CropPredictionResponse;
import com.cropdetector.backend.service.MlServiceClient;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    private final MlServiceClient mlServiceClient;

    public CropDiagnosisController(MlServiceClient mlServiceClient) {
        this.mlServiceClient = mlServiceClient;
    }

    /**
     * Public endpoint for frontend / mobile app to upload leaf photo.
     * POST /api/crops/diagnose
     * POST /api/v1/crops/diagnose
     */
    @PostMapping(value = "/diagnose", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CropPredictionResponse> diagnoseLeaf(
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "crop", required = false) String crop) throws IOException {

        MultipartFile targetImage = (image != null && !image.isEmpty()) ? image : file;
        if (targetImage == null || targetImage.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        CropPredictionResponse result = mlServiceClient.predictDisease(targetImage, crop);
        return ResponseEntity.ok(result);
    }
}
