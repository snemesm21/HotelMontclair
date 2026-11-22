package com.example.demo.dto;

import java.time.LocalDateTime;

public record ItineraryStopResponse(Long id, TouristPlaceResponse place, LocalDateTime addedAt) {
}
