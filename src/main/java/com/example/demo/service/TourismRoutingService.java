package com.example.demo.service;

import com.example.demo.dto.OptimizedRouteResponse;
import com.example.demo.dto.OptimizedRouteResponse.RoutePoint;
import com.example.demo.dto.RouteOptimizationRequest;
import com.example.demo.entities.ItineraryStop;
import com.example.demo.entities.TouristPlace;
import com.example.demo.errors.NotFoundException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TourismRoutingService {

    private static final String HOTEL_NAME = "Montclair Grand Hotel - L'Hermitage";
    private final TourismService tourismService;
    private final RoutingProvider routingProvider;
    private final String hotelAddress;
    private final String hotelLatitude;
    private final String hotelLongitude;

    public TourismRoutingService(
            TourismService tourismService,
            RoutingProvider routingProvider,
            @Value("${hotelmontclair.tourism.hotel.address:Montclair Grand Hotel - L'Hermitage, 98000 Monaco}")
            String hotelAddress,
            @Value("${hotelmontclair.tourism.hotel.latitude:}") String hotelLatitude,
            @Value("${hotelmontclair.tourism.hotel.longitude:}") String hotelLongitude) {
        this.tourismService = tourismService;
        this.routingProvider = routingProvider;
        this.hotelAddress = hotelAddress;
        this.hotelLatitude = hotelLatitude;
        this.hotelLongitude = hotelLongitude;
    }

    @Transactional(readOnly = true)
    public OptimizedRouteResponse optimize(Long clientId, RouteOptimizationRequest request) {
        RoutingMode mode = RoutingMode.from(request == null ? null : request.mode());
        boolean returnToHotel = request != null && Boolean.TRUE.equals(request.returnToHotel());
        List<ItineraryStop> stops = tourismService.getItineraryEntities(clientId);
        if (stops.isEmpty()) {
            throw new IllegalArgumentException("Agregue al menos un destino a su itinerario antes de optimizar la ruta.");
        }

        RoutingProvider.Coordinate hotel = configuredHotelCoordinate();
        List<RoutingProvider.Coordinate> coordinates = new ArrayList<>();
        coordinates.add(hotel);
        for (ItineraryStop stop : stops) {
            TouristPlace place = stop.getPlace();
            TourismService.validateCoordinate(place.getLatitude(), place.getLongitude());
            coordinates.add(new RoutingProvider.Coordinate(place.getLatitude(), place.getLongitude()));
        }
        if (returnToHotel) {
            coordinates.add(hotel);
        }

        double[][] distanceMatrix = routingProvider.getDistanceMatrix(coordinates, mode);
        validateDistanceMatrix(distanceMatrix, coordinates.size());
        List<Integer> routeIndexes = nearestNeighbor(distanceMatrix, stops.size(), returnToHotel);
        improveWithTwoOpt(routeIndexes, distanceMatrix, returnToHotel);

        List<RoutingProvider.Coordinate> orderedCoordinates = routeIndexes.stream()
                .map(coordinates::get)
                .toList();
        RoutingProvider.RoadRoute roadRoute = routingProvider.getRoute(orderedCoordinates, mode);
        List<RoutePoint> visits = buildVisits(routeIndexes, coordinates, stops, returnToHotel);
        return new OptimizedRouteResponse(
                mode.name(),
                returnToHotel,
                roadRoute.distanceMeters(),
                roadRoute.durationSeconds(),
                visits,
                roadRoute.geometry(),
                "LineString",
                googleMapsUrl(visits, mode, returnToHotel),
                "Recorrido optimizado partiendo desde MontClair Grand Hotel.");
    }

    private RoutingProvider.Coordinate configuredHotelCoordinate() {
        if (hotelLatitude == null || hotelLatitude.isBlank() || hotelLongitude == null || hotelLongitude.isBlank()) {
            throw new TourismRoutingConfigurationException(
                    "Configure HOTEL_TOURISM_LATITUDE y HOTEL_TOURISM_LONGITUDE con las coordenadas verificadas del hotel.");
        }
        try {
            double latitude = Double.parseDouble(hotelLatitude);
            double longitude = Double.parseDouble(hotelLongitude);
            TourismService.validateCoordinate(latitude, longitude);
            return new RoutingProvider.Coordinate(latitude, longitude);
        } catch (IllegalArgumentException exception) {
            throw new TourismRoutingConfigurationException(
                    "Las coordenadas configuradas para el hotel no son válidas.");
        }
    }

    private List<Integer> nearestNeighbor(double[][] distances, int destinationCount, boolean returnToHotel) {
        List<Integer> route = new ArrayList<>();
        route.add(0);
        boolean[] visited = new boolean[destinationCount + 1];
        int current = 0;
        for (int count = 0; count < destinationCount; count++) {
            int nearest = -1;
            double shortestDistance = Double.POSITIVE_INFINITY;
            for (int destinationIndex = 1; destinationIndex <= destinationCount; destinationIndex++) {
                if (!visited[destinationIndex] && distances[current][destinationIndex] < shortestDistance) {
                    nearest = destinationIndex;
                    shortestDistance = distances[current][destinationIndex];
                }
            }
            if (nearest < 0 || !Double.isFinite(shortestDistance)) {
                throw new TourismRoutingException("No existe una ruta vial entre el hotel y todos los destinos del itinerario.");
            }
            route.add(nearest);
            visited[nearest] = true;
            current = nearest;
        }
        if (returnToHotel) {
            route.add(destinationCount + 1);
        }
        return route;
    }

    private void improveWithTwoOpt(List<Integer> route, double[][] distances, boolean returnToHotel) {
        int lastMutableIndex = returnToHotel ? route.size() - 2 : route.size() - 1;
        boolean improved;
        do {
            improved = false;
            double currentCost = routeCost(route, distances);
            for (int start = 1; start < lastMutableIndex; start++) {
                for (int end = start + 1; end <= lastMutableIndex; end++) {
                    reverse(route, start, end);
                    double candidateCost = routeCost(route, distances);
                    if (candidateCost + 0.001 < currentCost) {
                        currentCost = candidateCost;
                        improved = true;
                    } else {
                        reverse(route, start, end);
                    }
                }
            }
        } while (improved);
    }

    private double routeCost(List<Integer> route, double[][] distances) {
        double cost = 0;
        for (int index = 1; index < route.size(); index++) {
            cost += distances[route.get(index - 1)][route.get(index)];
        }
        return cost;
    }

    private void reverse(List<Integer> route, int start, int end) {
        while (start < end) {
            Integer value = route.get(start);
            route.set(start++, route.get(end));
            route.set(end--, value);
        }
    }

    private void validateDistanceMatrix(double[][] matrix, int expectedSize) {
        if (matrix == null || matrix.length != expectedSize) {
            throw new TourismRoutingException("El proveedor devolvió una matriz de distancias inválida.");
        }
        for (double[] row : matrix) {
            if (row == null || row.length != expectedSize) {
                throw new TourismRoutingException("El proveedor devolvió una matriz de distancias incompleta.");
            }
            for (double distance : row) {
                if (Double.isNaN(distance) || distance < 0) {
                    throw new TourismRoutingException("El proveedor devolvió distancias no válidas.");
                }
            }
        }
    }

    private List<RoutePoint> buildVisits(
            List<Integer> routeIndexes,
            List<RoutingProvider.Coordinate> coordinates,
            List<ItineraryStop> stops,
            boolean returnToHotel) {
        List<RoutePoint> visits = new ArrayList<>();
        for (int index = 0; index < routeIndexes.size(); index++) {
            int coordinateIndex = routeIndexes.get(index);
            RoutingProvider.Coordinate coordinate = coordinates.get(coordinateIndex);
            if (coordinateIndex == 0 || (returnToHotel && coordinateIndex == coordinates.size() - 1)) {
                visits.add(new RoutePoint("HOTEL", null, null, HOTEL_NAME,
                        coordinate.latitude(), coordinate.longitude(), null, hotelMapsUrl()));
            } else {
                TouristPlace place = stops.get(coordinateIndex - 1).getPlace();
                visits.add(new RoutePoint("DESTINATION", index, place.getId(), place.getName(),
                        coordinate.latitude(), coordinate.longitude(),
                        place.getSuggestedDurationMinutes(), destinationMapsUrl(place)));
            }
        }
        return visits;
    }

    private String googleMapsUrl(List<RoutePoint> visits, RoutingMode mode, boolean returnToHotel) {
        List<RoutePoint> destinations = visits.stream().filter(point -> "DESTINATION".equals(point.type())).toList();
        RoutePoint destination = returnToHotel ? visits.get(visits.size() - 1) : destinations.get(destinations.size() - 1);
        List<RoutePoint> waypoints = returnToHotel
                ? destinations
                : destinations.subList(0, Math.max(0, destinations.size() - 1));
        String url = "https://www.google.com/maps/dir/?api=1&origin=" + encode(hotelAddress)
                + "&destination=" + encode(destination.latitude() + "," + destination.longitude());
        if (!waypoints.isEmpty()) {
            String waypointList = waypoints.stream()
                    .map(point -> point.latitude() + "," + point.longitude())
                    .reduce((first, second) -> first + "|" + second)
                    .orElse("");
            url += "&waypoints=" + encode(waypointList);
        }
        return url + "&travelmode=" + mode.googleMapsValue();
    }

    private String destinationMapsUrl(TouristPlace place) {
        return "https://www.google.com/maps/dir/?api=1&origin=" + encode(hotelAddress)
                + "&destination=" + encode(place.getLatitude() + "," + place.getLongitude());
    }

    private String hotelMapsUrl() {
        return "https://maps.app.goo.gl/hGMqVYHjjAP1ZwFLA";
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
