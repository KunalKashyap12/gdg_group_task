package com.cropdetector.backend.security;

public record GoogleTokenPayload(
    String sub,
    String email,
    boolean emailVerified,
    String name,
    String picture,
    String aud,
    String iss,
    long exp
) {}
