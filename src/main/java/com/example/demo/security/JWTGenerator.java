package com.example.demo.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;

@Component
public class JWTGenerator {
    private static final long EXPIRATION_TIME = 1000L * 60 * 60 * 24; // 24 horas
    private static final SecretKey KEY = Jwts.SIG.HS512.key().build();

    public String generateToken(Authentication authentication) {

        /* Datos necesarios para la creación */
        String username = authentication.getName();
        Date currentDate = new Date();
        Date expireDate = new Date(currentDate.getTime() + EXPIRATION_TIME);

        /* Crear el JWT */
        String token = Jwts.builder()
            .subject(username)
            .issuedAt(currentDate)
            .expiration(expireDate)
            .signWith(KEY)
            .compact();

        return token;
    }

    public String extractUsername(String token) {
        return Jwts.parser().verifyWith(KEY).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(KEY).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
