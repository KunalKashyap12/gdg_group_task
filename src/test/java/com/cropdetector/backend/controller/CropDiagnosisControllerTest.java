package com.cropdetector.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class CropDiagnosisControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    private MockMultipartFile validLeafImage;

    @BeforeEach
    void setUp() throws IOException {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

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
    void postDiagnoseReturnsSuccessWithPredictionAndTopK() throws Exception {
        mockMvc.perform(multipart("/api/crops/diagnose")
                .file(validLeafImage)
                .param("crop", "Tomato"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").isNotEmpty())
            .andExpect(jsonPath("$.is_leaf").value(true))
            .andExpect(jsonPath("$.prediction").exists())
            .andExpect(jsonPath("$.prediction.crop").isNotEmpty())
            .andExpect(jsonPath("$.prediction.confidence").isNumber())
            .andExpect(jsonPath("$.top_k").isArray())
            .andExpect(jsonPath("$.disclaimer").isNotEmpty());
    }

    @Test
    void postDiagnoseAcceptsFileFieldFallback() throws Exception {
        MockMultipartFile fallbackFile = new MockMultipartFile(
            "file", "leaf.png", "image/png", validLeafImage.getBytes());

        mockMvc.perform(multipart("/api/crops/diagnose")
                .file(fallbackFile)
                .param("crop", "Potato"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").isNotEmpty())
            .andExpect(jsonPath("$.prediction.crop").isNotEmpty());
    }

    @Test
    void postDiagnoseRejectsEmptyImage() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "image", "empty.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/api/crops/diagnose")
                .file(emptyFile)
                .param("crop", "Tomato"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void postDiagnoseUnderV1PathAlsoWorks() throws Exception {
        mockMvc.perform(multipart("/api/v1/crops/diagnose")
                .file(validLeafImage)
                .param("crop", "Apple"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").isNotEmpty())
            .andExpect(jsonPath("$.prediction.crop").isNotEmpty());
    }
}
