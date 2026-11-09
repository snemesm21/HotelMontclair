package com.example.demo.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OAuthTemporaryCodeStoreTest {

    @Test
    void exchangesCodeOnce() {
        OAuthTemporaryCodeStore store = new OAuthTemporaryCodeStore();
        String code = store.create(42L);

        assertEquals(42L, store.exchange(code));
        assertThrows(IllegalArgumentException.class, () -> store.exchange(code));
    }

    @Test
    void rejectsUnknownCode() {
        OAuthTemporaryCodeStore store = new OAuthTemporaryCodeStore();
        assertThrows(IllegalArgumentException.class, () -> store.exchange("unknown"));
    }
}
