package com.cropdetector.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.cropdetector.backend.dto.AuthDtos;
import com.cropdetector.backend.exception.ApiException;
import com.cropdetector.backend.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void registersNewUserSuccessfully() {
        var request = new AuthDtos.RegisterRequest(
            "farmer@example.com", "SecurePass123!", "Ramesh Kumar");

        var response = authService.register(request);

        assertNotNull(response);
        assertNotNull(response.accessToken());
        assertEquals("farmer@example.com", response.email());
        assertEquals("Bearer", response.tokenType());
        assertTrue(response.expiresInSeconds() > 0);
        assertNotNull(response.userId());
        assertTrue(userRepository.existsByEmail("farmer@example.com"));
    }

    @Test
    void rejectsDuplicateEmailRegistration() {
        var request1 = new AuthDtos.RegisterRequest(
            "farmer@example.com", "SecurePass123!", "Ramesh Kumar");
        authService.register(request1);

        var request2 = new AuthDtos.RegisterRequest(
            "farmer@example.com", "DifferentPass999!", "Ramesh Copy");

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(request2));
        assertEquals("EMAIL_EXISTS", ex.getCode());
    }

    @Test
    void logsInWithValidCredentials() {
        authService.register(new AuthDtos.RegisterRequest(
            "farmer@example.com", "SecurePass123!", "Ramesh Kumar"));

        var loginResponse = authService.login(new AuthDtos.LoginRequest(
            "farmer@example.com", "SecurePass123!"));

        assertNotNull(loginResponse);
        assertNotNull(loginResponse.accessToken());
        assertEquals("farmer@example.com", loginResponse.email());
    }

    @Test
    void rejectsLoginWithWrongPassword() {
        authService.register(new AuthDtos.RegisterRequest(
            "farmer@example.com", "SecurePass123!", "Ramesh Kumar"));

        ApiException ex = assertThrows(ApiException.class, () ->
            authService.login(new AuthDtos.LoginRequest("farmer@example.com", "WrongPassword!")));

        assertEquals("INVALID_CREDENTIALS", ex.getCode());
    }

    @Test
    void rejectsLoginWithNonexistentUser() {
        ApiException ex = assertThrows(ApiException.class, () ->
            authService.login(new AuthDtos.LoginRequest("unknown@example.com", "AnyPassword!")));

        assertEquals("INVALID_CREDENTIALS", ex.getCode());
    }
}
