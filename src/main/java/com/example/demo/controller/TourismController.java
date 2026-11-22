package com.example.demo.controller;

import com.example.demo.dto.ItineraryStopResponse;
import com.example.demo.dto.OptimizedRouteResponse;
import com.example.demo.dto.RouteOptimizationRequest;
import com.example.demo.dto.TouristPlaceRequest;
import com.example.demo.dto.TouristPlaceResponse;
import com.example.demo.service.JwtService;
import com.example.demo.service.TourismRoutingService;
import com.example.demo.service.TourismService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tourism")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class TourismController {

    private final TourismService tourismService;
    private final TourismRoutingService routingService;

    public TourismController(TourismService tourismService, TourismRoutingService routingService) {
        this.tourismService = tourismService;
        this.routingService = routingService;
    }

    @Operation(summary = "Lista el catálogo público de destinos turísticos de Mónaco")
    @GetMapping("/places")
    public ResponseEntity<List<TouristPlaceResponse>> getPlaces(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(tourismService.getPlaces(category));
    }

    @Operation(summary = "Obtiene el detalle público de un destino por slug")
    @GetMapping("/places/{slug}")
    public ResponseEntity<TouristPlaceResponse> getPlace(@PathVariable String slug) {
        return ResponseEntity.ok(tourismService.getPlace(slug));
    }

    @Operation(summary = "Amplía el catálogo turístico con un destino (ADMIN)")
    @PostMapping("/places")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TouristPlaceResponse> createPlace(@RequestBody TouristPlaceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tourismService.createPlace(request));
    }

    @Operation(summary = "Consulta el itinerario privado del huésped autenticado")
    @GetMapping("/itinerary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ItineraryStopResponse>> getItinerary() {
        return ResponseEntity.ok(tourismService.getItinerary(resolveClientId()));
    }

    @Operation(summary = "Agrega un destino al itinerario autenticado sin duplicarlo")
    @PostMapping("/itinerary/{placeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ItineraryStopResponse> addToItinerary(@PathVariable Long placeId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(tourismService.addToItinerary(resolveClientId(), placeId));
    }

    @Operation(summary = "Elimina un destino del itinerario autenticado")
    @DeleteMapping("/itinerary/{placeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeFromItinerary(@PathVariable Long placeId) {
        tourismService.removeFromItinerary(resolveClientId(), placeId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Optimiza el recorrido vial aproximado del itinerario autenticado")
    @PostMapping("/itinerary/optimize")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OptimizedRouteResponse> optimizeItinerary(
            @RequestBody(required = false) RouteOptimizationRequest request) {
        return ResponseEntity.ok(routingService.optimize(resolveClientId(), request));
    }

    private Long resolveClientId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof JwtService.JwtClaims claims)
                || claims.clientId() == null) {
            throw new AccessDeniedException("No se pudo identificar al huésped autenticado.");
        }
        return claims.clientId();
    }
}
