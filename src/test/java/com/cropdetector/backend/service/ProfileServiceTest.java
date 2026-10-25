package com.cropdetector.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.cropdetector.backend.dto.AuthDtos;
import com.cropdetector.backend.dto.PredictionDtos;
import com.cropdetector.backend.exception.ApiException;
import com.cropdetector.backend.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class ProfileServiceTest {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    private AuthDtos.AuthResponse authUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        authUser = authService.register(new AuthDtos.RegisterRequest(
            "kisan@example.com", "Password123!", "Kisan Kumar"));
    }

    @Test
    void getsAndUpdatesProfile() {
        java.util.UUID userId = java.util.UUID.fromString(authUser.userId());

        var profile = profileService.getProfile(userId);
        assertEquals("kisan@example.com", profile.email());
        assertEquals("Kisan Kumar", profile.fullName());

        var updated = profileService.updateProfile(userId,
            new AuthDtos.UpdateProfileRequest("Kisan Kumar Sharma"));
        assertEquals("Kisan Kumar Sharma", updated.fullName());
    }

    @Test
    void getsAndUpdatesPreferences() {
        java.util.UUID userId = java.util.UUID.fromString(authUser.userId());

        var prefs = profileService.getPreferences(userId);
        assertEquals("en", prefs.language());
        assertTrue(prefs.notificationsEnabled());

        var updatedPrefs = profileService.updatePreferences(userId,
            new PredictionDtos.PreferencesRequest("hi", true, true));
        assertEquals("hi", updatedPrefs.language());
        assertTrue(updatedPrefs.emailUpdatesEnabled());
    }

    @Test
    void rejectsUnsupportedLanguage() {
        java.util.UUID userId = java.util.UUID.fromString(authUser.userId());

        ApiException ex = assertThrows(ApiException.class, () ->
            profileService.updatePreferences(userId,
                new PredictionDtos.PreferencesRequest("fr", false, false)));

        assertEquals("INVALID_LANGUAGE", ex.getCode());
    }
}
