package com.example.demo.dto;

import java.util.List;

public record TouristPlaceResponse(
        Long id,
        String slug,
        String name,
        String description,
        String shortDescription,
        String longDescription,
        String category,
        List<String> imageUrls,
        List<String> imageAttributions,
        String address,
        double latitude,
        double longitude,
        int suggestedDurationMinutes,
        int displayOrder,
        String googleMapsUrl) {
}
