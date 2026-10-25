package com.cropdetector.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.cropdetector.backend.dto.PredictionDtos;
import com.cropdetector.backend.repository.PredictionRepository;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class PredictionServiceTest {

    @Autowired
    private PredictionService predictionService;

    @Autowired
    private PredictionRepository predictionRepository;

    private MockMultipartFile validLeafImage;

    @BeforeEach
    void setUp() throws IOException {
        predictionRepository.deleteAll();

        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.GREEN);
        g.fillRect(0, 0, 100, 100);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);

        validLeafImage = new MockMultipartFile(
            "image", "leaf.png", "image/png", baos.toByteArray());
    }

    @Test
    void predictsDiseaseForGuestUser() {
        PredictionDtos.PredictionResponse response =
            predictionService.predict(null, validLeafImage, "tomato");

        assertNotNull(response);
        assertNotNull(response.predictionId());
        assertEquals("tomato", response.crop());
        assertNotNull(response.predictedDisease());
        assertTrue(response.confidence() > 0);
        assertNotNull(response.status());
        assertNotNull(response.guidance());
        assertFalse(response.guidance().isEmpty());
    }

    @Test
    void recordsPredictionHistoryForAuthenticatedUser() {
        UUID userId = UUID.randomUUID();

        var p1 = predictionService.predict(userId, validLeafImage, "potato");
        var p2 = predictionService.predict(userId, validLeafImage, "apple");

        var historyPage = predictionService.history(userId, 0, 10);
        assertNotNull(historyPage);
        assertEquals(2, historyPage.getTotalElements());

        var fetched = predictionService.getById(UUID.fromString(p1.predictionId()), userId);
        assertNotNull(fetched);
        assertEquals(p1.predictionId(), fetched.predictionId());

        predictionService.delete(UUID.fromString(p1.predictionId()), userId);
        var updatedHistory = predictionService.history(userId, 0, 10);
        assertEquals(1, updatedHistory.getTotalElements());
    }
}
