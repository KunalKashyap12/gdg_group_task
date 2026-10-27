package com.cropdetector.backend.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final String predictionUrl;

    public HealthController(
        @Value("${ml.service.url:${app.ml.base-url:https://gdg-grp-task-ml-2.onrender.com}}")
        String mlServiceUrl
    ) {
        String baseUrl = mlServiceUrl.replaceAll("/+$", "");
        this.predictionUrl = baseUrl.endsWith("/api/v1/predict")
            ? baseUrl
            : (baseUrl.endsWith("/predict") ? baseUrl : baseUrl + "/api/v1/predict");
    }

    @GetMapping({"/actuator/health", "/api/v1/health"})
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "predictionUrl", predictionUrl
        ));
    }
}
