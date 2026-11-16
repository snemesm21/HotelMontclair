package com.example.demo.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final byte[] secret;

    public JwtService(@Value("${hotelmontclair.auth.jwt-secret:}") String configuredSecret) {
        String secretValue = configuredSecret;
        if (secretValue == null || secretValue.isBlank()) {
            byte[] generated = new byte[64];
            new SecureRandom().nextBytes(generated);
            secretValue = Base64.getEncoder().encodeToString(generated);
        }
        try {
            this.secret = MessageDigest.getInstance("SHA-256")
                    .digest(secretValue.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible inicializar la firma de sesión.", e);
        }
    }

    public String createToken(Long clientId, String email, String role) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(clientId))
                    .claim("email", email)
                    .claim("role", role)
                    .issuer("hotel-montclair")
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(7200)))
                    .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secret));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo crear la credencial de sesión.", e);
        }
    }

    public Optional<JwtClaims> validateToken(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new MACVerifier(secret))) {
                return Optional.empty();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expiration = claims.getExpirationTime();
            if (expiration == null || expiration.before(new Date())) {
                return Optional.empty();
            }
            return Optional.of(new JwtClaims(
                    Long.valueOf(claims.getSubject()),
                    (String) claims.getClaim("email"),
                    (String) claims.getClaim("role")));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public record JwtClaims(Long clientId, String email, String role) {
    }
}
