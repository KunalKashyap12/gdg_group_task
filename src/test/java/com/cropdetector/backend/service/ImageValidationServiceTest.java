package com.cropdetector.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.cropdetector.backend.exception.ApiException;

class ImageValidationServiceTest {

    private final ImageValidationService validator = new ImageValidationService();

    @Test
    void acceptsValidPngImage() throws IOException {
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.GREEN);
        g.fillRect(0, 0, 200, 200);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);

        MockMultipartFile file = new MockMultipartFile(
            "image", "leaf.png", "image/png", baos.toByteArray());

        var validated = validator.validate(file);
        assertNotNull(validated);
        assertEquals("image/png", validated.contentType());
    }

    @Test
    void acceptsValidJpegImage() throws IOException {
        BufferedImage image = new BufferedImage(150, 150, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.YELLOW);
        g.fillRect(0, 0, 150, 150);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", baos);

        MockMultipartFile file = new MockMultipartFile(
            "image", "leaf.jpg", "image/jpeg", baos.toByteArray());

        var validated = validator.validate(file);
        assertNotNull(validated);
        assertEquals("image/jpeg", validated.contentType());
    }

    @Test
    void rejectsEmptyImage() {
        MockMultipartFile file = new MockMultipartFile(
            "image", "empty.png", "image/png", new byte[0]);

        ApiException ex = assertThrows(ApiException.class, () -> validator.validate(file));
        assertEquals("EMPTY_IMAGE", ex.getCode());
    }

    @Test
    void rejectsFakePng() {
        MockMultipartFile file = new MockMultipartFile(
            "image", "fake.png", "image/png",
            "this is not an image".getBytes());

        ApiException ex = assertThrows(ApiException.class, () -> validator.validate(file));
        assertEquals("INVALID_IMAGE", ex.getCode());
    }

    @Test
    void rejectsUnsupportedSvg() {
        MockMultipartFile file = new MockMultipartFile(
            "image", "leaf.svg", "image/svg+xml",
            "<svg></svg>".getBytes());

        ApiException ex = assertThrows(ApiException.class, () -> validator.validate(file));
        assertEquals("INVALID_IMAGE", ex.getCode());
    }

    @Test
    void rejectsExorbitantDimensions() throws IOException {
        // Create an image with dimension > 6000
        BufferedImage image = new BufferedImage(6001, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);

        MockMultipartFile file = new MockMultipartFile(
            "image", "oversized.png", "image/png", baos.toByteArray());

        ApiException ex = assertThrows(ApiException.class, () -> validator.validate(file));
        assertEquals("IMAGE_DIMENSIONS_INVALID", ex.getCode());
    }
}
