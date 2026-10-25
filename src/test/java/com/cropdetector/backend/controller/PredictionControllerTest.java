package com.cropdetector.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class PredictionControllerTest {

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
    void postPredictionReturnsResultAndGuidance() throws Exception {
        mockMvc.perform(multipart("/api/v1/predictions")
                .file(validLeafImage)
                .param("crop", "tomato"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.predictionId").isNotEmpty())
            .andExpect(jsonPath("$.crop").value("tomato"))
            .andExpect(jsonPath("$.predictedDisease").isNotEmpty())
            .andExpect(jsonPath("$.confidence").isNumber())
            .andExpect(jsonPath("$.status").isNotEmpty())
            .andExpect(jsonPath("$.guidance").isArray())
            .andExpect(jsonPath("$.expertConfirmationRecommended").value(true));
    }

    @Test
    void postPredictionRejectsEmptyImage() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "image", "empty.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/api/v1/predictions")
                .file(emptyFile)
                .param("crop", "tomato"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("EMPTY_IMAGE"));
    }

    @Test
    void apiDocumentationEndpointsReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/docs"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));

        mockMvc.perform(get("/api/v1/docs/openapi.json"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.openapi").value("3.0.3"));
    }
}
