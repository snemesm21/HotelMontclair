package com.example.demo.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OAuthTemporaryCodeStore {

    private final Map<String, PendingExchange> exchanges = new ConcurrentHashMap<>();

    public String create(Long clientId) {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        exchanges.put(code, new PendingExchange(clientId, Instant.now().plusSeconds(60)));
        return code;
    }

    public Long exchange(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("El código temporal es obligatorio.");
        }
        PendingExchange exchange = exchanges.remove(code);
        if (exchange == null) {
            throw new IllegalArgumentException("El código temporal es inválido o ya fue utilizado.");
        }
        if (Instant.now().isAfter(exchange.expiresAt())) {
            throw new IllegalArgumentException("El código temporal expiró.");
        }
        return exchange.clientId();
    }

    private record PendingExchange(Long clientId, Instant expiresAt) {
    }
}
