package com.example.demo.dto;

import java.util.List;

public record TouristPlaceResponse(
        Long id,
        String slug,
        String name,
        String description,
        String category,
        List<String> imageUrls,
        String address,
        double latitude,
        double longitude,
        int suggestedDurationMinutes,
        String googleMapsUrl) {
}
