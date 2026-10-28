
package com.cropdetector.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 100) String fullName
    ) {}

    public record LoginRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 72) String password
    ) {}

    public record GoogleAuthRequest(
        String idToken,
        String credential,
        String token
    ) {
        public String resolveToken() {
            if (idToken != null && !idToken.isBlank()) return idToken.trim();
            if (credential != null && !credential.isBlank()) return credential.trim();
            if (token != null && !token.isBlank()) return token.trim();
            return null;
        }
    }

    public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String userId,
        String email
    ) {}

    public record ProfileResponse(
        String userId,
        String email,
        String fullName,
        String authProvider,
        String pictureUrl
    ) {
        public ProfileResponse(String userId, String email, String fullName) {
            this(userId, email, fullName, "LOCAL", null);
        }
    }

    public record SendOtpRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 254)
        String email,

        String purpose
    ) {}

    public record VerifyOtpRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 254)
        String email,

        @NotBlank(message = "OTP code is required")
        @Size(min = 4, max = 10, message = "OTP must be between 4 and 10 digits")
        String otp,

        @Size(max = 100)
        String fullName
    ) {}

    public record OtpResponse(
        String status,
        String message,
        String email,
        long expiresInSeconds,
        String otp
    ) {}

    public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String fullName
    ) {}
}
