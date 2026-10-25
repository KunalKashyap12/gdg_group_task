package com.cropdetector.backend.security;

public interface GoogleTokenVerifier {

    /**
     * Verifies a Google ID token and returns the verified payload claims.
     *
     * @param idToken the raw Google ID token JWT string
     * @return verified GoogleTokenPayload containing email, name, sub, picture, etc.
     * @throws com.cropdetector.backend.exception.ApiException if token is invalid, expired, or rejected
     */
    GoogleTokenPayload verify(String idToken);
}
