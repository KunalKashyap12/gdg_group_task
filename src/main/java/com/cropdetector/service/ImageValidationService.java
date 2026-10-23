
package com.cropdetector.backend.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import com.cropdetector.backend.exception.ApiException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageValidationService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final long MAX_PIXELS = 20_000_000L;
    private static final int MAX_DIMENSION = 6000;

    public record ValidatedImage(byte[] bytes, String contentType) {}

    public ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                "EMPTY_IMAGE", "Please upload a leaf image.");
        }

        if (file.getSize() > MAX_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,
                "FILE_TOO_LARGE", "Maximum image size is 5 MB.");
        }

        String type = file.getContentType();
        if (!"image/jpeg".equalsIgnoreCase(type) &&
            !"image/png".equalsIgnoreCase(type)) {
            throw invalidImage();
        }

        try {
            byte[] bytes = file.getBytes();

            boolean jpeg = bytes.length >= 3 &&
                (bytes[0] & 0xFF) == 0xFF &&
                (bytes[1] & 0xFF) == 0xD8 &&
                (bytes[2] & 0xFF) == 0xFF;

            boolean png = bytes.length >= 8 &&
                Arrays.equals(
                    Arrays.copyOf(bytes, 8),
                    new byte[] {
                        (byte) 0x89, 0x50, 0x4E, 0x47,
                        0x0D, 0x0A, 0x1A, 0x0A
                    });

            if (!jpeg && !png) throw invalidImage();

            BufferedImage image = ImageIO.read(
                new ByteArrayInputStream(bytes));

            if (image == null) throw invalidImage();

            int width = image.getWidth();
            int height = image.getHeight();

            if (width <= 0 || height <= 0 ||
                width > MAX_DIMENSION || height > MAX_DIMENSION ||
                (long) width * height > MAX_PIXELS) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                    "IMAGE_DIMENSIONS_INVALID",
                    "Image dimensions exceed the allowed limits.");
            }

            return new ValidatedImage(
                bytes, jpeg ? "image/jpeg" : "image/png");

        } catch (IOException ex) {
            throw invalidImage();
        }
    }

    private ApiException invalidImage() {
        return new ApiException(HttpStatus.BAD_REQUEST,
            "INVALID_IMAGE",
            "Upload a valid JPEG or PNG image.");
    }
}
