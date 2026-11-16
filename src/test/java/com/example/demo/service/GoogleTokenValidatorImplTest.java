package com.example.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoogleTokenValidatorImplTest {

    private GoogleTokenValidatorImpl validator;
    private NimbusJwtDecoder decoder;

    @BeforeEach
    void setUp() {
        validator = new GoogleTokenValidatorImpl(
                "client-id",
                "https://accounts.google.com",
                "https://www.googleapis.com/oauth2/v3/certs");
        decoder = mock(NimbusJwtDecoder.class);
        ReflectionTestUtils.setField(validator, "decoder", decoder);
    }

    private Jwt validToken() {
        return token(true, "expected-nonce", "https://accounts.google.com",
                Instant.now().plusSeconds(300), List.of("client-id"));
    }

    private Jwt token(boolean verified, String nonce, String issuer, Instant expires, List<String> audience) {
        Instant now = Instant.now();
        Instant issuedAt = expires.isBefore(now) ? expires.minusSeconds(300) : now;
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("google-subject")
                .issuer(issuer)
                .audience(audience)
                .issuedAt(issuedAt)
                .expiresAt(expires)
                .claim("email", "google@gmail.com")
                .claim("email_verified", verified)
                .claim("nonce", nonce)
                .claim("name", "Google User")
                .build();
    }

    @Test
    void acceptsValidClaims() {
        when(decoder.decode("valid")).thenReturn(validToken());
        validator.validate("valid", "expected-nonce");
    }

    @Test
    void rejectsWrongAudience() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("google-subject")
                .issuer("https://accounts.google.com")
                .audience(List.of("other-client"))
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("email", "google@gmail.com")
                .claim("email_verified", true)
                .claim("nonce", "expected-nonce")
                .build();
        when(decoder.decode("wrong-audience")).thenReturn(jwt);
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("wrong-audience", "expected-nonce"));
    }

    @Test
    void rejectsExpiredToken() {
        Jwt jwt = token(true, "expected-nonce", "https://accounts.google.com",
                Instant.now().minusSeconds(1), List.of("client-id"));
        when(decoder.decode("expired")).thenReturn(jwt);
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("expired", "expected-nonce"));
    }

    @Test
    void rejectsUnverifiedEmail() {
        Jwt jwt = token(false, "expected-nonce", "https://accounts.google.com",
                Instant.now().plusSeconds(300), List.of("client-id"));
        when(decoder.decode("unverified")).thenReturn(jwt);
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("unverified", "expected-nonce"));
    }

    @Test
    void rejectsWrongNonce() {
        when(decoder.decode("wrong-nonce")).thenReturn(validToken());
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("wrong-nonce", "other-nonce"));
    }

    @Test
    void rejectsWrongIssuer() {
        Jwt jwt = token(true, "expected-nonce", "https://malicious.example.com",
                Instant.now().plusSeconds(300), List.of("client-id"));
        when(decoder.decode("wrong-issuer")).thenReturn(jwt);
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("wrong-issuer", "expected-nonce"));
    }
}
