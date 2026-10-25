
package com.cropdetector.backend.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.cropdetector.backend.exception.ApiException;

class ImageValidationServiceTest {

    private final ImageValidationService validator =
        new ImageValidationService();

    @Test
    void rejectsEmptyImage() {
        MockMultipartFile file = new MockMultipartFile(
            "image", "empty.png", "image/png", new byte[0]);

        assertThrows(ApiException.class, () -> validator.validate(file));
    }

    @Test
    void rejectsFakePng() {
        MockMultipartFile file = new MockMultipartFile(
            "image", "fake.png", "image/png",
            "this is not an image".getBytes());

        assertThrows(ApiException.class, () -> validator.validate(file));
    }

    @Test
    void rejectsUnsupportedSvg() {
        MockMultipartFile file = new MockMultipartFile(
            "image", "leaf.svg", "image/svg+xml",
            "<svg></svg>".getBytes());

        assertThrows(ApiException.class, () -> validator.validate(file));
    }
}
