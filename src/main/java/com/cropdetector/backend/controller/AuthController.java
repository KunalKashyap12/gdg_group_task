
package com.cropdetector.backend.controller;

import com.cropdetector.backend.dto.AuthDtos;
import com.cropdetector.backend.service.AuthService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.AuthResponse register(
        @Valid @RequestBody AuthDtos.RegisterRequest request
    ) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(
        @Valid @RequestBody AuthDtos.LoginRequest request
    ) {
        return authService.login(request);
    }

    @PostMapping("/google")
    public AuthDtos.AuthResponse googleLogin(
        @RequestBody(required = false) AuthDtos.GoogleAuthRequest request
    ) {
        String token = request != null ? request.resolveToken() : null;
        if (token == null || token.isBlank()) {
            throw new com.cropdetector.backend.exception.ApiException(
                HttpStatus.BAD_REQUEST, "INVALID_INPUT",
                "Google ID token is required in 'idToken' or 'credential'.");
        }
        return authService.loginWithGoogle(token);
    }

    @PostMapping({"/otp/send", "/send-otp"})
    public AuthDtos.OtpResponse sendOtp(
        @Valid @RequestBody AuthDtos.SendOtpRequest request
    ) {
        return authService.sendOtp(request);
    }

    @PostMapping({"/otp/verify", "/verify-otp"})
    public AuthDtos.AuthResponse verifyOtp(
        @Valid @RequestBody AuthDtos.VerifyOtpRequest request
    ) {
        return authService.verifyOtp(request);
    }
}
