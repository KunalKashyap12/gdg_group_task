
package com.cropdetector.backend.service;

import java.util.Locale;

import com.cropdetector.backend.dto.AuthDtos;
import com.cropdetector.backend.model.AppUser;
import com.cropdetector.backend.model.UserPreferences;
import com.cropdetector.backend.repository.UserPreferencesRepository;
import com.cropdetector.backend.repository.UserRepository;
import com.cropdetector.backend.security.JwtService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final UserPreferencesRepository preferences;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
        UserRepository users,
        UserPreferencesRepository preferences,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService
    ) {
        this.users = users;
        this.preferences = preferences;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (users.existsByEmail(email)) {
            throw new com.cropdetector.backend.exception.ApiException(
                HttpStatus.CONFLICT, "EMAIL_EXISTS",
                "An account with this email already exists.");
        }

        AppUser user = new AppUser(
            email, passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());

        try {
            user = users.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new com.cropdetector.backend.exception.ApiException(
                HttpStatus.CONFLICT, "EMAIL_EXISTS",
                "An account with this email already exists.");
        }

        preferences.save(new UserPreferences(user.getId()));

        return createResponse(user);
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        String email = normalizeEmail(request.email());

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    email, request.password()));
        } catch (AuthenticationException ex) {
            throw new com.cropdetector.backend.exception.ApiException(
                HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                "Email or password is incorrect.");
        }

        AppUser user = users.findByEmail(email)
            .orElseThrow(() ->
                new com.cropdetector.backend.exception.ApiException(
                    HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                    "Email or password is incorrect."));

        return createResponse(user);
    }

    private AuthDtos.AuthResponse createResponse(AppUser user) {
        return new AuthDtos.AuthResponse(
            jwtService.generateToken(user.getEmail()),
            "Bearer",
            jwtService.getExpirationSeconds(),
            user.getId().toString(),
            user.getEmail());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
