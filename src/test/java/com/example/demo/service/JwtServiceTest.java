package com.example.demo.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    @Test
    void createsAndValidatesOwnSessionToken() {
        JwtService service = new JwtService("test-secret-with-at-least-32-bytes-for-hmac-signing");
        String token = service.createToken(12L, "client@gmail.com", "CLIENT");

        var claims = service.validateToken(token);
        assertTrue(claims.isPresent());
        assertEquals(12L, claims.get().clientId());
        assertEquals("CLIENT", claims.get().role());
    }

    @Test
    void rejectsInvalidSessionToken() {
        JwtService service = new JwtService("test-secret-with-at-least-32-bytes-for-hmac-signing");
        assertTrue(service.validateToken("invalid.token").isEmpty());
    }
}
