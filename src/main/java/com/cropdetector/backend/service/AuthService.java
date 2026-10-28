
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
    private final com.cropdetector.backend.security.GoogleTokenVerifier googleTokenVerifier;
    private final OtpService otpService;

    public AuthService(
        UserRepository users,
        UserPreferencesRepository preferences,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        com.cropdetector.backend.security.GoogleTokenVerifier googleTokenVerifier,
        OtpService otpService
    ) {
        this.users = users;
        this.preferences = preferences;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.googleTokenVerifier = googleTokenVerifier;
        this.otpService = otpService;
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

        // Check if user exists but registered via Google OAuth without password
        users.findByEmail(email).ifPresent(existingUser -> {
            if (existingUser.getPasswordHash() == null) {
                throw new com.cropdetector.backend.exception.ApiException(
                    HttpStatus.UNAUTHORIZED, "OAUTH_USER_NO_PASSWORD",
                    "This account was registered using Google Sign-In. Please sign in with Google.");
            }
        });

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

    @Transactional
    public AuthDtos.AuthResponse loginWithGoogle(String idToken) {
        com.cropdetector.backend.security.GoogleTokenPayload payload =
            googleTokenVerifier.verify(idToken);
        String email = normalizeEmail(payload.email());

        AppUser user = users.findByEmail(email).orElse(null);

        if (user != null) {
            boolean updated = false;
            if (user.getGoogleSub() == null && payload.sub() != null) {
                user.setGoogleSub(payload.sub());
                updated = true;
            }
            if (user.getPictureUrl() == null && payload.picture() != null) {
                user.setPictureUrl(payload.picture());
                updated = true;
            }
            if ((user.getFullName() == null || user.getFullName().isBlank()) &&
                payload.name() != null && !payload.name().isBlank()) {
                user.setFullName(payload.name().trim());
                updated = true;
            }
            if (updated) {
                user = users.save(user);
            }
        } else {
            String fullName = payload.name() != null && !payload.name().isBlank()
                ? payload.name().trim()
                : email.split("@")[0];

            user = new AppUser(
                email,
                fullName,
                "GOOGLE",
                payload.sub(),
                payload.picture()
            );

            try {
                user = users.saveAndFlush(user);
            } catch (DataIntegrityViolationException ex) {
                user = users.findByEmail(email)
                    .orElseThrow(() -> new com.cropdetector.backend.exception.ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "USER_CREATION_FAILED",
                        "Failed to initialize user account."));
            }

            preferences.save(new UserPreferences(user.getId()));
        }

        return createResponse(user);
    }

    @Transactional
    public AuthDtos.OtpResponse sendOtp(AuthDtos.SendOtpRequest request) {
        String email = normalizeEmail(request.email());
        String purpose = request.purpose() != null ? request.purpose() : "LOGIN";
        return otpService.generateAndSendOtp(email, purpose);
    }

    @Transactional
    public AuthDtos.AuthResponse verifyOtp(AuthDtos.VerifyOtpRequest request) {
        String email = normalizeEmail(request.email());
        otpService.verifyOtp(email, request.otp());

        AppUser user = users.findByEmail(email).orElse(null);

        if (user != null) {
            if (!Boolean.TRUE.equals(user.getEmailVerified())) {
                user.setEmailVerified(true);
                user = users.save(user);
            }
        } else {
            String fullName = (request.fullName() != null && !request.fullName().isBlank())
                ? request.fullName().trim()
                : email.split("@")[0];

            user = new AppUser(
                email,
                fullName,
                "OTP",
                null,
                null
            );
            user.setEmailVerified(true);

            try {
                user = users.saveAndFlush(user);
            } catch (DataIntegrityViolationException ex) {
                user = users.findByEmail(email)
                    .orElseThrow(() -> new com.cropdetector.backend.exception.ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "USER_CREATION_FAILED",
                        "Failed to initialize user account."));
            }

            preferences.save(new UserPreferences(user.getId()));
        }

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
