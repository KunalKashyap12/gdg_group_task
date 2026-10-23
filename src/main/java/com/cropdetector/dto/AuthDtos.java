
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
        String fullName
    ) {}

    public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String fullName
    ) {}
}
