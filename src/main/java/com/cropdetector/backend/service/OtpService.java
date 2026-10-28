package com.cropdetector.backend.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.cropdetector.backend.dto.AuthDtos;
import com.cropdetector.backend.exception.ApiException;
import com.cropdetector.backend.model.OtpVerification;
import com.cropdetector.backend.repository.OtpVerificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final int OTP_EXPIRY_SECONDS = 300; // 5 minutes
    private static final int RESEND_COOLDOWN_SECONDS = 30; // 30 seconds
    private static final int MAX_ATTEMPTS = 5;

    private final OtpVerificationRepository otpRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.mock-response:true}")
    private boolean includeOtpInResponse;

    public OtpService(OtpVerificationRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    @Transactional
    public AuthDtos.OtpResponse generateAndSendOtp(String email, String purpose) {
        String normalizedEmail = normalizeEmail(email);
        String resolvedPurpose = (purpose != null && !purpose.isBlank())
            ? purpose.trim().toUpperCase(Locale.ROOT)
            : "LOGIN";

        // Check rate limiting cooldown (e.g. 30s)
        Optional<OtpVerification> latestUnverified =
            otpRepository.findTopByEmailAndVerifiedFalseOrderByCreatedAtDesc(normalizedEmail);

        if (latestUnverified.isPresent()) {
            OtpVerification existing = latestUnverified.get();
            long elapsedSeconds = Duration.between(existing.getCreatedAt(), Instant.now()).getSeconds();
            if (elapsedSeconds < RESEND_COOLDOWN_SECONDS) {
                long waitTime = RESEND_COOLDOWN_SECONDS - elapsedSeconds;
                throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "OTP_RATE_LIMITED",
                    "Please wait " + waitTime + " seconds before requesting another code."
                );
            }
        }

        // Invalidate previous unverified OTPs for this email
        List<OtpVerification> previousOtps = otpRepository.findAllByEmail(normalizedEmail);
        for (OtpVerification prev : previousOtps) {
            if (!prev.isVerified()) {
                prev.setVerified(true);
            }
        }
        if (!previousOtps.isEmpty()) {
            otpRepository.saveAll(previousOtps);
        }

        // Generate 6-digit numeric OTP (100000 - 999999)
        int codeNumber = 100000 + secureRandom.nextInt(900000);
        String otpCode = String.valueOf(codeNumber);
        Instant expiresAt = Instant.now().plusSeconds(OTP_EXPIRY_SECONDS);

        OtpVerification newOtp = new OtpVerification(normalizedEmail, otpCode, resolvedPurpose, expiresAt);
        otpRepository.save(newOtp);

        // Console banner logging for developers/testing
        log.info("================================================================================");
        log.info("[OTP VERIFICATION CODE]");
        log.info("Destination: {}", normalizedEmail);
        log.info("OTP Code:    {}", otpCode);
        log.info("Purpose:     {}", resolvedPurpose);
        log.info("Expires in:  {} seconds (5 minutes)", OTP_EXPIRY_SECONDS);
        log.info("================================================================================");

        String otpForResponse = includeOtpInResponse ? otpCode : null;

        return new AuthDtos.OtpResponse(
            "success",
            "Verification code sent successfully to " + normalizedEmail,
            normalizedEmail,
            OTP_EXPIRY_SECONDS,
            otpForResponse
        );
    }

    @Transactional
    public OtpVerification verifyOtp(String email, String rawCode) {
        String normalizedEmail = normalizeEmail(email);

        if (rawCode == null || rawCode.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OTP", "OTP code cannot be blank.");
        }
        String code = rawCode.trim();

        OtpVerification otpEntry = otpRepository
            .findTopByEmailAndVerifiedFalseOrderByCreatedAtDesc(normalizedEmail)
            .orElseThrow(() -> new ApiException(
                HttpStatus.BAD_REQUEST,
                "OTP_NOT_FOUND",
                "No active OTP found for this email. Please request a new OTP."
            ));

        if (otpEntry.isExpired()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "OTP_EXPIRED",
                "OTP verification code has expired. Please request a new code."
            );
        }

        if (otpEntry.getAttempts() >= MAX_ATTEMPTS) {
            throw new ApiException(
                HttpStatus.TOO_MANY_REQUESTS,
                "MAX_ATTEMPTS_EXCEEDED",
                "Too many incorrect attempts. Please request a new OTP code."
            );
        }

        if (!otpEntry.getOtpCode().equals(code)) {
            otpEntry.incrementAttempts();
            otpRepository.save(otpEntry);
            int remaining = MAX_ATTEMPTS - otpEntry.getAttempts();
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "INVALID_OTP",
                "Invalid verification code. " + (remaining > 0 ? remaining + " attempts remaining." : "Please request a new code.")
            );
        }

        otpEntry.setVerified(true);
        return otpRepository.save(otpEntry);
    }

    private String normalizeEmail(String email) {
        if (email == null) return "";
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
