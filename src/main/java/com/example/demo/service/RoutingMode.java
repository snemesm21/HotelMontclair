package com.example.demo.service;

public enum RoutingMode {
    WALKING,
    DRIVING;

    public static RoutingMode from(String value) {
        if (value == null || value.isBlank()) {
            return DRIVING;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Modo inválido. Use WALKING o DRIVING.");
        }
    }

    public String googleMapsValue() {
        return this == WALKING ? "walking" : "driving";
    }
}
