package com.example.demo.controller;

import com.example.demo.dto.TouristPlaceResponse;
import com.example.demo.service.TourismService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tourist-places")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class TouristPlacesController {

    private final TourismService tourismService;

    public TouristPlacesController(TourismService tourismService) {
        this.tourismService = tourismService;
    }

    @Operation(summary = "Lista los destinos turísticos activos")
    @GetMapping
    public ResponseEntity<List<TouristPlaceResponse>> getPlaces(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(tourismService.getCatalogPlaces(category));
    }

    @Operation(summary = "Obtiene un destino turístico activo por ID")
    @GetMapping("/{id}")
    public ResponseEntity<TouristPlaceResponse> getPlace(@PathVariable Long id) {
        return ResponseEntity.ok(tourismService.getPlaceById(id));
    }
}
