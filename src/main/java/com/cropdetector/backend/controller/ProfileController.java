
package com.cropdetector.backend.controller;

import java.util.UUID;

import com.cropdetector.backend.dto.AuthDtos;
import com.cropdetector.backend.dto.PredictionDtos;
import com.cropdetector.backend.model.AppUser;
import com.cropdetector.backend.service.ProfileService;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public AuthDtos.ProfileResponse getProfile(
        @AuthenticationPrincipal AppUser user
    ) {
        return profileService.getProfile(user.getId());
    }

    @PatchMapping
    public AuthDtos.ProfileResponse updateProfile(
        @AuthenticationPrincipal AppUser user,
        @Valid @RequestBody AuthDtos.UpdateProfileRequest request
    ) {
        return profileService.updateProfile(user.getId(), request);
    }

    @GetMapping("/preferences")
    public PredictionDtos.PreferencesResponse getPreferences(
        @AuthenticationPrincipal AppUser user
    ) {
        return profileService.getPreferences(user.getId());
    }

    @PutMapping("/preferences")
    public PredictionDtos.PreferencesResponse updatePreferences(
        @AuthenticationPrincipal AppUser user,
        @Valid @RequestBody PredictionDtos.PreferencesRequest request
    ) {
        return profileService.updatePreferences(user.getId(), request);
    }
}
