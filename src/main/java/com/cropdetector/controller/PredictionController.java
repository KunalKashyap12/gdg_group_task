
package com.cropdetector.backend.controller;

import java.util.UUID;

import com.cropdetector.backend.dto.PredictionDtos;
import com.cropdetector.backend.exception.ApiException;
import com.cropdetector.backend.model.AppUser;
import com.cropdetector.backend.service.PredictionService;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/predictions")
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public PredictionDtos.PredictionResponse predict(
        @AuthenticationPrincipal AppUser user,
        @RequestPart("image") MultipartFile image,
        @RequestParam(required = false) String crop
    ) {
        return predictionService.predict(user.getId(), image, crop);
    }

    @GetMapping
    public Page<PredictionDtos.PredictionResponse> history(
        @AuthenticationPrincipal AppUser user,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        return predictionService.history(user.getId(), page, size);
    }

    @GetMapping("/{id}")
    public PredictionDtos.PredictionResponse getPrediction(
        @AuthenticationPrincipal AppUser user,
        @PathVariable String id
    ) {
        return predictionService.getById(parseId(id), user.getId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePrediction(
        @AuthenticationPrincipal AppUser user,
        @PathVariable String id
    ) {
        predictionService.delete(parseId(id), user.getId());
    }

    private UUID parseId(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "INVALID_ID", "Prediction ID is invalid.");
        }
    }
}
