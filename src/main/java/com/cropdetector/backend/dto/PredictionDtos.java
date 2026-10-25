
package com.cropdetector.backend.dto;

import java.time.Instant;
import java.util.List;

public final class PredictionDtos {
    private PredictionDtos() {}

    public record PredictionResponse(
        String predictionId,
        String crop,
        String predictedDisease,
        double confidence,
        String status,
        String modelVersion,
        List<String> guidance,
        boolean expertConfirmationRecommended,
        Instant createdAt
    ) {}

    public record PreferencesRequest(
        String language,
        boolean notificationsEnabled,
        boolean emailUpdatesEnabled
    ) {}

    public record PreferencesResponse(
        String language,
        boolean notificationsEnabled,
        boolean emailUpdatesEnabled
    ) {}

    public record ErrorResponse(
        String code,
        String message,
        Instant timestamp
    ) {}
}
