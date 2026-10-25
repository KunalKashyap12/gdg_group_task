package com.cropdetector.backend.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

import com.cropdetector.backend.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifierImpl.class);

    private final RestClient restClient;
    private final String expectedClientId;

    @Autowired
    public GoogleTokenVerifierImpl(
        @Value("${app.google.client-id:}") String expectedClientId,
        @Value("${app.google.token-info-url:https://oauth2.googleapis.com/tokeninfo}") String tokenInfoUrl,
        @Value("${app.google.connect-timeout-ms:3000}") int connectTimeout,
        @Value("${app.google.read-timeout-ms:5000}") int readTimeout
    ) {
        this.expectedClientId = expectedClientId != null ? expectedClientId.trim() : "";

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        factory.setReadTimeout(Duration.ofMillis(readTimeout));

        this.restClient = RestClient.builder()
            .baseUrl(tokenInfoUrl)
            .requestFactory(factory)
            .build();
    }

    // Constructor for testing with customized RestClient
    public GoogleTokenVerifierImpl(RestClient restClient, String expectedClientId) {
        this.restClient = restClient;
        this.expectedClientId = expectedClientId != null ? expectedClientId.trim() : "";
    }

    @Override
    public GoogleTokenPayload verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST, "INVALID_INPUT", "Google ID token is required.");
        }

        if (expectedClientId.isEmpty()) {
            log.error("Google authentication failed: app.google.client-id is not configured.");
            throw new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR, "GOOGLE_AUTH_NOT_CONFIGURED",
                "Google Client ID is not configured on the server.");
        }

        Map<String, Object> tokenInfo;
        try {
            tokenInfo = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .queryParam("id_token", idToken.trim())
                    .build())
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientResponseException ex) {
            HttpStatusCode status = ex.getStatusCode();
            if (status.is4xxClientError()) {
                log.warn("Google token verification rejected by tokeninfo endpoint: status={}", status);
                throw new ApiException(
                    HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN",
                    "Invalid or expired Google ID token.");
            }
            log.error("Google tokeninfo service returned server error: status={}", status);
            throw new ApiException(
                HttpStatus.BAD_GATEWAY, "GOOGLE_SERVICE_ERROR",
                "Failed to verify token with Google.");
        } catch (Exception ex) {
            log.error("Failed to reach Google tokeninfo endpoint: {}", ex.getMessage());
            throw new ApiException(
                HttpStatus.BAD_GATEWAY, "GOOGLE_SERVICE_UNAVAILABLE",
                "Google verification service is currently unreachable.");
        }

        if (tokenInfo == null || tokenInfo.isEmpty()) {
            throw new ApiException(
                HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN",
                "Empty response from Google tokeninfo service.");
        }

        // Validate issuer
        String iss = (String) tokenInfo.get("iss");
        if (iss == null || (!iss.equals("accounts.google.com") && !iss.equals("https://accounts.google.com"))) {
            throw new ApiException(
                HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN",
                "Google token has invalid issuer.");
        }

        // Validate audience matches configured Client ID (supports comma-separated list of client IDs)
        String aud = (String) tokenInfo.get("aud");
        boolean audMatched = aud != null && Arrays.stream(expectedClientId.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .anyMatch(aud::equals);

        if (!audMatched) {
            log.warn("Google token audience mismatch. Expected: [{}], Received: [{}]", expectedClientId, aud);
            throw new ApiException(
                HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_AUDIENCE",
                "Google token audience does not match configured Client ID.");
        }

        // Validate email_verified
        Object emailVerifiedObj = tokenInfo.get("email_verified");
        boolean emailVerified = emailVerifiedObj != null && Boolean.parseBoolean(String.valueOf(emailVerifiedObj));
        if (!emailVerified) {
            throw new ApiException(
                HttpStatus.UNAUTHORIZED, "UNVERIFIED_GOOGLE_EMAIL",
                "Google account email is not verified.");
        }

        String email = (String) tokenInfo.get("email");
        if (email == null || email.isBlank()) {
            throw new ApiException(
                HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN",
                "Google token does not contain an email address.");
        }

        // Check expiration if present
        long exp = 0;
        try {
            Object expObj = tokenInfo.get("exp");
            if (expObj != null) {
                exp = Long.parseLong(String.valueOf(expObj));
                if (exp > 0 && Instant.now().getEpochSecond() > exp) {
                    throw new ApiException(
                        HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN",
                        "Google token has expired.");
                }
            }
        } catch (NumberFormatException ignored) {}

        String sub = (String) tokenInfo.get("sub");
        String name = (String) tokenInfo.get("name");
        String picture = (String) tokenInfo.get("picture");

        return new GoogleTokenPayload(
            sub, email, emailVerified, name, picture, aud, iss, exp);
    }
}
