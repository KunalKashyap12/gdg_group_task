package com.cropdetector.backend.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.time.Instant;

import com.cropdetector.backend.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GoogleTokenVerifierTest {

    private static final String CLIENT_ID = "1234567890-testclient.apps.googleusercontent.com";
    private static final String BASE_URL = "https://oauth2.googleapis.com/tokeninfo";

    private MockRestServiceServer mockServer;
    private GoogleTokenVerifierImpl verifier;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        verifier = new GoogleTokenVerifierImpl(restClient, CLIENT_ID);
    }

    @Test
    void verifiesValidTokenSuccessfully() {
        long futureExp = Instant.now().getEpochSecond() + 3600;
        String googleResponseJson = String.format("""
            {
                "iss": "https://accounts.google.com",
                "sub": "110169484474386276334",
                "aud": "%s",
                "email": "farmer.ramesh@gmail.com",
                "email_verified": "true",
                "name": "Ramesh Patel",
                "picture": "https://lh3.googleusercontent.com/a/avatar.jpg",
                "exp": "%d"
            }
            """, CLIENT_ID, futureExp);

        mockServer.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=valid-jwt-token"))
            .andRespond(withSuccess(googleResponseJson, MediaType.APPLICATION_JSON));

        GoogleTokenPayload payload = verifier.verify("valid-jwt-token");

        assertNotNull(payload);
        assertEquals("farmer.ramesh@gmail.com", payload.email());
        assertEquals("Ramesh Patel", payload.name());
        assertEquals("110169484474386276334", payload.sub());
        assertEquals("https://lh3.googleusercontent.com/a/avatar.jpg", payload.picture());
        assertTrue(payload.emailVerified());
        assertEquals(CLIENT_ID, payload.aud());
        mockServer.verify();
    }

    @Test
    void supportsMultiClientIdConfiguration() {
        String multipleClientIds = CLIENT_ID + ", 987654321-android.apps.googleusercontent.com";
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GoogleTokenVerifierImpl multiVerifier = new GoogleTokenVerifierImpl(builder.build(), multipleClientIds);

        long futureExp = Instant.now().getEpochSecond() + 3600;
        String googleResponseJson = String.format("""
            {
                "iss": "https://accounts.google.com",
                "sub": "110169484474386276334",
                "aud": "987654321-android.apps.googleusercontent.com",
                "email": "farmer.ramesh@gmail.com",
                "email_verified": "true",
                "name": "Ramesh Patel",
                "exp": "%d"
            }
            """, futureExp);

        server.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=android-token"))
            .andRespond(withSuccess(googleResponseJson, MediaType.APPLICATION_JSON));

        GoogleTokenPayload payload = multiVerifier.verify("android-token");
        assertNotNull(payload);
        assertEquals("987654321-android.apps.googleusercontent.com", payload.aud());
        server.verify();
    }

    @Test
    void rejectsMissingOrBlankToken() {
        ApiException ex1 = assertThrows(ApiException.class, () -> verifier.verify(""));
        assertEquals("INVALID_INPUT", ex1.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, ex1.getStatus());

        ApiException ex2 = assertThrows(ApiException.class, () -> verifier.verify(null));
        assertEquals("INVALID_INPUT", ex2.getCode());
    }

    @Test
    void rejectsWhenServerClientIdNotConfigured() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        GoogleTokenVerifierImpl unconfigured = new GoogleTokenVerifierImpl(builder.build(), "");

        ApiException ex = assertThrows(ApiException.class, () -> unconfigured.verify("any-token"));
        assertEquals("GOOGLE_AUTH_NOT_CONFIGURED", ex.getCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
    }

    @Test
    void rejectsInvalidOrExpiredTokenFromGoogle() {
        mockServer.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=bad-token"))
            .andRespond(withBadRequest().body("{\"error\": \"invalid_token\"}").contentType(MediaType.APPLICATION_JSON));

        ApiException ex = assertThrows(ApiException.class, () -> verifier.verify("bad-token"));
        assertEquals("INVALID_GOOGLE_TOKEN", ex.getCode());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        mockServer.verify();
    }

    @Test
    void rejectsAudienceMismatch() {
        long futureExp = Instant.now().getEpochSecond() + 3600;
        String googleResponseJson = String.format("""
            {
                "iss": "https://accounts.google.com",
                "sub": "110169484474386276334",
                "aud": "wrong-client-id.apps.googleusercontent.com",
                "email": "farmer.ramesh@gmail.com",
                "email_verified": "true",
                "exp": "%d"
            }
            """, futureExp);

        mockServer.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=token-with-wrong-aud"))
            .andRespond(withSuccess(googleResponseJson, MediaType.APPLICATION_JSON));

        ApiException ex = assertThrows(ApiException.class, () -> verifier.verify("token-with-wrong-aud"));
        assertEquals("INVALID_GOOGLE_AUDIENCE", ex.getCode());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        mockServer.verify();
    }

    @Test
    void rejectsUnverifiedEmail() {
        long futureExp = Instant.now().getEpochSecond() + 3600;
        String googleResponseJson = String.format("""
            {
                "iss": "https://accounts.google.com",
                "sub": "110169484474386276334",
                "aud": "%s",
                "email": "unverified@gmail.com",
                "email_verified": "false",
                "exp": "%d"
            }
            """, CLIENT_ID, futureExp);

        mockServer.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=unverified-token"))
            .andRespond(withSuccess(googleResponseJson, MediaType.APPLICATION_JSON));

        ApiException ex = assertThrows(ApiException.class, () -> verifier.verify("unverified-token"));
        assertEquals("UNVERIFIED_GOOGLE_EMAIL", ex.getCode());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        mockServer.verify();
    }

    @Test
    void rejectsExpiredTokenClaim() {
        long pastExp = Instant.now().getEpochSecond() - 60; // 1 minute in the past
        String googleResponseJson = String.format("""
            {
                "iss": "https://accounts.google.com",
                "sub": "110169484474386276334",
                "aud": "%s",
                "email": "farmer.ramesh@gmail.com",
                "email_verified": "true",
                "exp": "%d"
            }
            """, CLIENT_ID, pastExp);

        mockServer.expect(requestTo("https://oauth2.googleapis.com/tokeninfo?id_token=expired-token"))
            .andRespond(withSuccess(googleResponseJson, MediaType.APPLICATION_JSON));

        ApiException ex = assertThrows(ApiException.class, () -> verifier.verify("expired-token"));
        assertEquals("INVALID_GOOGLE_TOKEN", ex.getCode());
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        mockServer.verify();
    }
}
