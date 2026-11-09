package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class GoogleTokenValidatorImpl implements GoogleTokenValidator {

    private final String clientId;
    private final String issuer;
    private final NimbusJwtDecoder decoder;

    public GoogleTokenValidatorImpl(
            @Value("${hotelmontclair.google.client-id:${GOOGLE_CLIENT_ID:}}") String clientId,
            @Value("${hotelmontclair.google.issuer:https://accounts.google.com}") String issuer,
            @Value("${hotelmontclair.google.jwks-uri:https://www.googleapis.com/oauth2/v3/certs}") String jwksUri) {
        this.clientId = clientId;
        this.issuer = issuer;
        this.decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        this.decoder.setJwtValidator(token -> validateClaims(token, null));
    }

    @Override
    public GoogleUserInfo validate(String idToken, String expectedNonce) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("GOOGLE_CLIENT_ID no está configurado.");
        }
        try {
            Jwt token = decoder.decode(idToken);
            OAuth2TokenValidatorResult claimsResult = validateClaims(token, expectedNonce);
            if (claimsResult.hasErrors()) {
                OAuth2Error error = claimsResult.getErrors().iterator().next();
                throw new IllegalArgumentException(error.getDescription());
            }

            String subject = token.getSubject();
            String email = token.getClaimAsString("email");
            Boolean verified = token.getClaim("email_verified");
            if (email == null || email.isBlank() || !Boolean.TRUE.equals(verified)) {
                throw new IllegalArgumentException("Google debe proporcionar un correo verificado.");
            }

            String name = token.getClaimAsString("name");
            String givenName = token.getClaimAsString("given_name");
            String familyName = token.getClaimAsString("family_name");
            String firstName = givenName != null ? givenName : (name != null ? name : email);
            String lastName = familyName != null ? familyName : "";
            String avatarUrl = token.getClaimAsString("picture");
            return new GoogleUserInfo(subject, email, firstName, lastName, avatarUrl);
        } catch (JwtException e) {
            throw new IllegalArgumentException("El token de Google es inválido o expiró.", e);
        }
    }

    private OAuth2TokenValidatorResult validateClaims(Jwt token, String expectedNonce) {
        if (token.getExpiresAt() != null && token.getExpiresAt().isBefore(Instant.now())) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("token_expired", "El token de Google expiró.", null));
        }
        String tokenIssuer = token.getIssuer() == null ? null : token.getIssuer().toString();
        if (!"https://accounts.google.com".equals(tokenIssuer)
                && !"accounts.google.com".equals(tokenIssuer)
                && !issuer.equals(tokenIssuer)) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_issuer", "El emisor del token de Google es inválido.", null));
        }

        List<String> audiences = token.getAudience();
        if (audiences == null || !audiences.contains(clientId)) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_audience", "La audiencia del token de Google es inválida.", null));
        }

        if (expectedNonce != null) {
            String tokenNonce = token.getClaimAsString("nonce");
            if (!expectedNonce.equals(tokenNonce)) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_nonce", "El nonce del token de Google es inválido.", null));
            }
        }

        if (token.getSubject() == null || token.getSubject().isBlank()) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_subject", "El token de Google no contiene un identificador válido.", null));
        }

        return OAuth2TokenValidatorResult.success();
    }
}
