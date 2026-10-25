
package com.cropdetector.backend.service;

import java.util.Locale;
import java.util.UUID;

import com.cropdetector.backend.dto.AuthDtos;
import com.cropdetector.backend.dto.PredictionDtos;
import com.cropdetector.backend.exception.ApiException;
import com.cropdetector.backend.model.AppUser;
import com.cropdetector.backend.model.UserPreferences;
import com.cropdetector.backend.repository.UserPreferencesRepository;
import com.cropdetector.backend.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserRepository users;
    private final UserPreferencesRepository preferences;

    public ProfileService(UserRepository users,
                          UserPreferencesRepository preferences) {
        this.users = users;
        this.preferences = preferences;
    }

    @Transactional(readOnly = true)
    public AuthDtos.ProfileResponse getProfile(UUID userId) {
        AppUser user = findUser(userId);

        return new AuthDtos.ProfileResponse(
            user.getId().toString(),
            user.getEmail(),
            user.getFullName(),
            user.getAuthProvider(),
            user.getPictureUrl());
    }

    @Transactional
    public AuthDtos.ProfileResponse updateProfile(
        UUID userId, AuthDtos.UpdateProfileRequest request
    ) {
        AppUser user = findUser(userId);
        user.setFullName(request.fullName().trim());
        users.save(user);

        return new AuthDtos.ProfileResponse(
            user.getId().toString(),
            user.getEmail(),
            user.getFullName(),
            user.getAuthProvider(),
            user.getPictureUrl());
    }

    @Transactional(readOnly = true)
    public PredictionDtos.PreferencesResponse getPreferences(UUID userId) {
        UserPreferences p = findPreferences(userId);
        return toPreferencesResponse(p);
    }

    @Transactional
    public PredictionDtos.PreferencesResponse updatePreferences(
        UUID userId, PredictionDtos.PreferencesRequest request
    ) {
        if (request.language() == null ||
            !java.util.Set.of("en", "hi").contains(
                request.language().trim().toLowerCase(Locale.ROOT))) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "INVALID_LANGUAGE", "Language must be en or hi.");
        }

        UserPreferences p = findPreferences(userId);
        p.setLanguage(request.language().trim().toLowerCase(Locale.ROOT));
        p.setNotificationsEnabled(request.notificationsEnabled());
        p.setEmailUpdatesEnabled(request.emailUpdatesEnabled());

        return toPreferencesResponse(preferences.save(p));
    }

    private AppUser findUser(UUID id) {
        return users.findById(id).orElseThrow(() ->
            new ApiException(HttpStatus.NOT_FOUND,
                "USER_NOT_FOUND", "User not found."));
    }

    private UserPreferences findPreferences(UUID userId) {
        return preferences.findByUserId(userId).orElseThrow(() ->
            new ApiException(HttpStatus.NOT_FOUND,
                "PREFERENCES_NOT_FOUND", "User preferences not found."));
    }

    private PredictionDtos.PreferencesResponse toPreferencesResponse(
        UserPreferences p
    ) {
        return new PredictionDtos.PreferencesResponse(
            p.getLanguage(),
            p.isNotificationsEnabled(),
            p.isEmailUpdatesEnabled());
    }
}
