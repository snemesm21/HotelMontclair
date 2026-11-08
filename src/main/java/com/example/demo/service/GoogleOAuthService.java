package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GoogleOAuthService {

    private final String clientId;
    private final String clientSecret;
    private final String authorizationUri;
    private final String tokenUri;
    private final String redirectUri;
    private final GoogleTokenValidator tokenValidator;
    private final GoogleAccountService accountService;
    private final OAuthTemporaryCodeStore temporaryCodeStore;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ConcurrentHashMap<String, PendingFlow> flows = new ConcurrentHashMap<>();

    public GoogleOAuthService(
            @Value("${hotelmontclair.google.client-id}") String clientId,
            @Value("${hotelmontclair.google.client-secret}") String clientSecret,
            @Value("${hotelmontclair.google.authorization-uri:https://accounts.google.com/o/oauth2/v2/auth}") String authorizationUri,
            @Value("${hotelmontclair.google.token-uri:https://oauth2.googleapis.com/token}") String tokenUri,
            @Value("${hotelmontclair.google.redirect-uri:http://localhost:8080/login/oauth2/code/google}")             String redirectUri,
            GoogleTokenValidator tokenValidator,
            GoogleAccountService accountService,
            OAuthTemporaryCodeStore temporaryCodeStore) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authorizationUri = authorizationUri;
        this.tokenUri = tokenUri;
        this.redirectUri = redirectUri;
        this.tokenValidator = tokenValidator;
        this.accountService = accountService;
        this.temporaryCodeStore = temporaryCodeStore;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder().build();
    }

    public String createAuthorizationUrl() {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("GOOGLE_CLIENT_ID no está configurado.");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("GOOGLE_CLIENT_SECRET no está configurado.");
        }

        String state = randomUrlSafe(32);
        String nonce = randomUrlSafe(32);
        String codeVerifier = randomUrlSafe(64);
        String codeChallenge = codeChallenge(codeVerifier);
        flows.put(state, new PendingFlow(nonce, codeVerifier, Instant.now().plusSeconds(600)));

        return UriComponentsBuilder.fromUriString(authorizationUri)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", state)
                .queryParam("nonce", nonce)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .queryParam("prompt", "select_account")
                .build().encode().toUriString();
    }

    public String completeAuthorization(String code, String state, String error) {
        if (error != null && !error.isBlank()) {
            throw new IllegalArgumentException("El proveedor Google rechazó la autenticación: " + error);
        }
        PendingFlow flow = flows.remove(state);
        if (flow == null) {
            throw new IllegalArgumentException("El estado OAuth es inválido o ya fue utilizado.");
        }
        if (Instant.now().isAfter(flow.expiresAt())) {
            throw new IllegalArgumentException("El estado OAuth expiró.");
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Google no devolvió un código de autorización.");
        }

        String idToken;
        try {
            String form = "code=" + encode(code)
                    + "&client_id=" + encode(clientId)
                    + "&client_secret=" + encode(clientSecret)
                    + "&redirect_uri=" + encode(redirectUri)
                    + "&grant_type=authorization_code"
                    + "&code_verifier=" + encode(flow.codeVerifier());

            HttpRequest request = HttpRequest.newBuilder(URI.create(tokenUri))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Google rechazó el intercambio del código.");
            }

            JsonNode tokenResponse = objectMapper.readTree(response.body());
            idToken = tokenResponse.path("id_token").asText(null);
            if (idToken == null || idToken.isBlank()) {
                throw new IllegalStateException("Google no devolvió un token de identidad.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La comunicación con Google fue interrumpida.", e);
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
                throw (RuntimeException) e;
            }
            throw new IllegalStateException("No fue posible contactar al proveedor Google.", e);
        }

        GoogleTokenValidator.GoogleUserInfo userInfo =
                tokenValidator.validate(idToken, flow.nonce());
        var client = accountService.findOrCreate(userInfo);
        String exchangeCode = temporaryCodeStore.create(client.getId());
        return exchangeCode;
    }

    public com.example.demo.entities.Client exchangeTemporaryCode(String exchangeCode) {
        Long clientId = temporaryCodeStore.exchange(exchangeCode);
        return accountService.findOrCreateFromId(clientId);
    }

    private static String randomUrlSafe(int bytes) {
        byte[] random = new byte[bytes];
        new SecureRandom().nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private static String codeChallenge(String verifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(digest.digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible crear PKCE.", e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record PendingFlow(String nonce, String codeVerifier, Instant expiresAt) {
    }

}
