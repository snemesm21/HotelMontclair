package com.example.demo.dto;

import java.util.List;

public record TouristPlaceRequest(
        String slug,
        String name,
        String description,
        String category,
        List<String> imageUrls,
        String address,
        Double latitude,
        Double longitude,
        Integer suggestedDurationMinutes) {
}
