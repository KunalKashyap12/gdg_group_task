package com.cropdetector.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

class HealthControllerTest {

    @Test
    void includesMlPredictionUrlInHealthResponse() {
        HealthController controller = new HealthController(
            "https://gdg-grp-task-ml-2.onrender.com");

        assertEquals(
            Map.of(
                "status", "UP",
                "predictionUrl", "https://gdg-grp-task-ml-2.onrender.com/api/v1/predict"
            ),
            controller.health().getBody()
        );
    }

    @Test
    void doesNotAppendPredictionPathWhenAlreadyConfigured() {
        HealthController controller = new HealthController(
            "https://gdg-grp-task-ml-2.onrender.com/api/v1/predict/");

        assertEquals(
            "https://gdg-grp-task-ml-2.onrender.com/api/v1/predict",
            controller.health().getBody().get("predictionUrl")
        );
    }
}
