package com.cropdetector.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class CropControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void listsAllSupportedCrops() throws Exception {
        mockMvc.perform(get("/api/v1/crops"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[?(@.id == 'tomato')]").exists())
            .andExpect(jsonPath("$[?(@.id == 'potato')]").exists())
            .andExpect(jsonPath("$[?(@.id == 'apple')]").exists())
            .andExpect(jsonPath("$[?(@.id == 'corn')]").exists());
    }

    @Test
    void getsTomatoCropDetails() throws Exception {
        mockMvc.perform(get("/api/v1/crops/tomato"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("tomato"))
            .andExpect(jsonPath("$.name").value("Tomato"))
            .andExpect(jsonPath("$.supportedDiseases").isArray())
            .andExpect(jsonPath("$.captureTips").isArray())
            .andExpect(jsonPath("$.preventiveTips").isArray());
    }

    @Test
    void returns404ForUnknownCrop() throws Exception {
        mockMvc.perform(get("/api/v1/crops/unknown-crop"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CROP_NOT_FOUND"));
    }
}
