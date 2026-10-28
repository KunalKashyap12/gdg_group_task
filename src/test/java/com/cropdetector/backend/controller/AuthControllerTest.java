package com.cropdetector.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.cropdetector.backend.exception.ApiException;
import com.cropdetector.backend.repository.UserRepository;
import com.cropdetector.backend.security.GoogleTokenPayload;
import com.cropdetector.backend.security.GoogleTokenVerifier;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class AuthControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private GoogleTokenVerifier googleTokenVerifier;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        userRepository.deleteAll();
    }

    @Test
    void registerEndpointReturnsCreatedAndJwt() throws Exception {
        String json = """
            {
                "email": "farmer.ram@example.com",
                "password": "Password123!",
                "fullName": "Ram Singh"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.email").value("farmer.ram@example.com"));
    }

    @Test
    void loginEndpointAuthenticatesSuccessfully() throws Exception {
        String registerJson = """
            {
                "email": "farmer.ram@example.com",
                "password": "Password123!",
                "fullName": "Ram Singh"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson))
            .andExpect(status().isCreated());

        String loginJson = """
            {
                "email": "farmer.ram@example.com",
                "password": "Password123!"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.email").value("farmer.ram@example.com"));
    }

    @Test
    void registerRejectsInvalidEmailFormat() throws Exception {
        String json = """
            {
                "email": "invalid-email",
                "password": "Password123!",
                "fullName": "Ram Singh"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void googleAuthEndpointAuthenticatesWithIdToken() throws Exception {
        when(googleTokenVerifier.verify("valid-google-id-token")).thenReturn(
            new GoogleTokenPayload(
                "google-sub-555",
                "farmer.google@example.com",
                true,
                "Google Farmer",
                "https://photos.google.com/pic.jpg",
                "test-client-id",
                "https://accounts.google.com",
                9999999999L
            )
        );

        String json = "{\"idToken\": \"valid-google-id-token\"}";

        mockMvc.perform(post("/api/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.email").value("farmer.google@example.com"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void googleAuthEndpointSupportsCredentialField() throws Exception {
        when(googleTokenVerifier.verify("gis-credential-token")).thenReturn(
            new GoogleTokenPayload(
                "google-sub-666",
                "gis.farmer@example.com",
                true,
                "GIS Farmer",
                null,
                "test-client-id",
                "https://accounts.google.com",
                9999999999L
            )
        );

        String json = "{\"credential\": \"gis-credential-token\"}";

        mockMvc.perform(post("/api/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.email").value("gis.farmer@example.com"));
    }

    @Test
    void googleAuthEndpointRejectsMissingToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void googleAuthEndpointRejectsInvalidToken() throws Exception {
        when(googleTokenVerifier.verify("invalid-token")).thenThrow(
            new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "Invalid or expired Google ID token."));

        mockMvc.perform(post("/api/v1/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\": \"invalid-token\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_GOOGLE_TOKEN"));
    }

    @Test
    void sendOtpAndVerifyOtpEndpointsWorkEndToEnd() throws Exception {
        String sendJson = """
            {
                "email": "farmer.controller@example.com",
                "purpose": "LOGIN"
            }
            """;

        var sendResult = mockMvc.perform(post("/api/v1/auth/otp/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sendJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.email").value("farmer.controller@example.com"))
            .andExpect(jsonPath("$.otp").isNotEmpty())
            .andReturn();

        String responseBody = sendResult.getResponse().getContentAsString();
        String otpCode = responseBody.replaceAll(".*\"otp\":\"([^\"]+)\".*", "$1");

        String verifyJson = String.format("""
            {
                "email": "farmer.controller@example.com",
                "otp": "%s",
                "fullName": "Controller Farmer"
            }
            """, otpCode);

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(verifyJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.email").value("farmer.controller@example.com"));
    }

    @Test
    void sendOtpAliasEndpointsWork() throws Exception {
        String sendJson = """
            {
                "email": "alias.farmer@example.com"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sendJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.email").value("alias.farmer@example.com"));
    }
}
