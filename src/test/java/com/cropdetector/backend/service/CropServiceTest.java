package com.cropdetector.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.cropdetector.backend.exception.ApiException;
import com.cropdetector.backend.model.Crop;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class CropServiceTest {

    @Autowired
    private CropService cropService;

    @Test
    void loadsSupportedCropsWithGuidance() {
        List<Crop> crops = cropService.getAllCrops();

        assertNotNull(crops);
        assertFalse(crops.isEmpty());
        assertTrue(crops.stream().anyMatch(c -> c.getId().equals("tomato")));
        assertTrue(crops.stream().anyMatch(c -> c.getId().equals("potato")));
        assertTrue(crops.stream().anyMatch(c -> c.getId().equals("apple")));
        assertTrue(crops.stream().anyMatch(c -> c.getId().equals("corn")));
    }

    @Test
    void fetchesTomatoDetailsWithCaptureAndPreventiveTips() {
        Crop tomato = cropService.getCropById("tomato");

        assertNotNull(tomato);
        assertEquals("Tomato", tomato.getName());
        assertFalse(tomato.getSupportedDiseases().isEmpty());
        assertFalse(tomato.getCaptureTips().isEmpty());
        assertFalse(tomato.getPreventiveTips().isEmpty());
    }

    @Test
    void throwsNotFoundForUnknownCrop() {
        ApiException ex = assertThrows(ApiException.class, () -> cropService.getCropById("dragonfruit"));
        assertEquals("CROP_NOT_FOUND", ex.getCode());
    }
}
