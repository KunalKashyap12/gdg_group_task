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
import com.cropdetector.backend.model.AppUser;
import com.cropdetector.backend.repository.UserRepository;
import com.cropdetector.backend.security.GoogleTokenPayload;
import com.cropdetector.backend.security.GoogleTokenVerifier;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private GoogleTokenVerifier googleTokenVerifier;

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

    @Test
    void registersAndAuthenticatesNewGoogleUser() {
        when(googleTokenVerifier.verify("google-token-new")).thenReturn(
            new GoogleTokenPayload(
                "sub-google-111",
                "google.farmer@example.com",
                true,
                "Suresh Farmer",
                "https://photos.google.com/avatar.jpg",
                "test-client-id",
                "https://accounts.google.com",
                9999999999L
            )
        );

        var response = authService.loginWithGoogle("google-token-new");

        assertNotNull(response);
        assertNotNull(response.accessToken());
        assertEquals("google.farmer@example.com", response.email());
        assertEquals("Bearer", response.tokenType());

        AppUser savedUser = userRepository.findByEmail("google.farmer@example.com").orElse(null);
        assertNotNull(savedUser);
        assertEquals("Suresh Farmer", savedUser.getFullName());
        assertEquals("GOOGLE", savedUser.getAuthProvider());
        assertEquals("sub-google-111", savedUser.getGoogleSub());
        assertEquals("https://photos.google.com/avatar.jpg", savedUser.getPictureUrl());
        assertNull(savedUser.getPasswordHash());
    }

    @Test
    void linksGoogleAccountToExistingUser() {
        authService.register(new AuthDtos.RegisterRequest(
            "existing.farmer@example.com", "Password123!", "Existing Farmer"));

        when(googleTokenVerifier.verify("google-token-existing")).thenReturn(
            new GoogleTokenPayload(
                "sub-google-222",
                "existing.farmer@example.com",
                true,
                "Existing Farmer Google",
                "https://photos.google.com/existing.jpg",
                "test-client-id",
                "https://accounts.google.com",
                9999999999L
            )
        );

        var response = authService.loginWithGoogle("google-token-existing");

        assertNotNull(response);
        assertEquals("existing.farmer@example.com", response.email());

        AppUser updatedUser = userRepository.findByEmail("existing.farmer@example.com").orElse(null);
        assertNotNull(updatedUser);
        assertEquals("sub-google-222", updatedUser.getGoogleSub());
        assertEquals("https://photos.google.com/existing.jpg", updatedUser.getPictureUrl());
    }

    @Test
    void rejectsPasswordLoginForGoogleOnlyAccount() {
        when(googleTokenVerifier.verify("google-token-333")).thenReturn(
            new GoogleTokenPayload(
                "sub-google-333",
                "oauth.only@example.com",
                true,
                "OAuth Only",
                null,
                "test-client-id",
                "https://accounts.google.com",
                9999999999L
            )
        );

        authService.loginWithGoogle("google-token-333");

        ApiException ex = assertThrows(ApiException.class, () ->
            authService.login(new AuthDtos.LoginRequest("oauth.only@example.com", "AttemptPassword!")));

        assertEquals("OAUTH_USER_NO_PASSWORD", ex.getCode());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void sendsOtpSuccessfully() {
        var response = authService.sendOtp(new AuthDtos.SendOtpRequest("farmer.otp@example.com", "LOGIN"));
        assertNotNull(response);
        assertEquals("success", response.status());
        assertEquals("farmer.otp@example.com", response.email());
        assertNotNull(response.otp());
        assertEquals(6, response.otp().length());
    }

    @Test
    void verifiesOtpAndRegistersNewUser() {
        var sendRes = authService.sendOtp(new AuthDtos.SendOtpRequest("new.otp.user@example.com", "REGISTER"));
        String code = sendRes.otp();

        var verifyRes = authService.verifyOtp(new AuthDtos.VerifyOtpRequest(
            "new.otp.user@example.com", code, "New OTP Farmer"));

        assertNotNull(verifyRes);
        assertNotNull(verifyRes.accessToken());
        assertEquals("new.otp.user@example.com", verifyRes.email());

        AppUser created = userRepository.findByEmail("new.otp.user@example.com").orElse(null);
        assertNotNull(created);
        assertEquals("New OTP Farmer", created.getFullName());
        assertEquals("OTP", created.getAuthProvider());
        assertTrue(created.getEmailVerified());
    }

    @Test
    void verifiesOtpAndLogsInExistingUser() {
        authService.register(new AuthDtos.RegisterRequest(
            "existing.otp.user@example.com", "Password123!", "Existing User"));

        var sendRes = authService.sendOtp(new AuthDtos.SendOtpRequest("existing.otp.user@example.com", "LOGIN"));
        String code = sendRes.otp();

        var verifyRes = authService.verifyOtp(new AuthDtos.VerifyOtpRequest(
            "existing.otp.user@example.com", code, null));

        assertNotNull(verifyRes);
        assertEquals("existing.otp.user@example.com", verifyRes.email());

        AppUser user = userRepository.findByEmail("existing.otp.user@example.com").orElse(null);
        assertNotNull(user);
        assertTrue(user.getEmailVerified());
    }

    @Test
    void rejectsInvalidOtpCode() {
        authService.sendOtp(new AuthDtos.SendOtpRequest("fail.otp@example.com", "LOGIN"));

        ApiException ex = assertThrows(ApiException.class, () ->
            authService.verifyOtp(new AuthDtos.VerifyOtpRequest("fail.otp@example.com", "000000", null)));

        assertEquals("INVALID_OTP", ex.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void rateLimitsRapidConsecutiveOtpRequests() {
        authService.sendOtp(new AuthDtos.SendOtpRequest("spam.otp@example.com", "LOGIN"));

        ApiException ex = assertThrows(ApiException.class, () ->
            authService.sendOtp(new AuthDtos.SendOtpRequest("spam.otp@example.com", "LOGIN")));

        assertEquals("OTP_RATE_LIMITED", ex.getCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
    }
}
