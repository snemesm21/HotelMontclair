package com.example.demo.dto;

import java.util.List;

public record OptimizedRouteResponse(
        String mode,
        boolean returnToHotel,
        double distanceMeters,
        double durationSeconds,
        List<RoutePoint> visits,
        List<List<Double>> geometry,
        String geometryType,
        String googleMapsUrl,
        String optimizationNote) {

    public record RoutePoint(
            String type,
            Integer visitOrder,
            Long placeId,
            String name,
            double latitude,
            double longitude,
            Integer suggestedDurationMinutes,
            String googleMapsUrl) {
    }
}
