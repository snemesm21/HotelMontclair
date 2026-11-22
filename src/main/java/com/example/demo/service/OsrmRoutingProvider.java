package com.example.demo.service;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OsrmRoutingProvider implements RoutingProvider {

    private final RestClient restClient;
    private final String drivingBaseUrl;
    private final String walkingBaseUrl;

    public OsrmRoutingProvider(
            @Value("${hotelmontclair.tourism.routing.driving-url:https://router.project-osrm.org}") String drivingBaseUrl,
            @Value("${hotelmontclair.tourism.routing.walking-url:https://routing.openstreetmap.de/routed-foot}") String walkingBaseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        this.drivingBaseUrl = trimTrailingSlash(drivingBaseUrl);
        this.walkingBaseUrl = trimTrailingSlash(walkingBaseUrl);
    }

    @Override
    public double[][] getDistanceMatrix(List<Coordinate> coordinates, RoutingMode mode) {
        try {
            String coordinatePath = toCoordinatePath(coordinates);
            String url = baseUrl(mode) + "/table/v1/driving/" + coordinatePath
                    + "?annotations=distance%2Cduration";
            JsonNode response = getJson(url, "La matriz de distancias no está disponible.");
            ensureSuccess(response, "La matriz de distancias no está disponible.");
            JsonNode distances = response.path("distances");
            if (!distances.isArray() || distances.size() != coordinates.size()) {
                throw new TourismRoutingException("El proveedor devolvió una matriz de distancias inválida.");
            }
            double[][] matrix = new double[distances.size()][distances.size()];
            for (int row = 0; row < distances.size(); row++) {
                JsonNode values = distances.get(row);
                if (!values.isArray() || values.size() != distances.size()) {
                    throw new TourismRoutingException("El proveedor devolvió una matriz de distancias incompleta.");
                }
                for (int column = 0; column < values.size(); column++) {
                    JsonNode value = values.get(column);
                    matrix[row][column] = value.isNumber() ? value.asDouble() : Double.POSITIVE_INFINITY;
                }
            }
            return matrix;
        } catch (Exception ex) {
            return fallbackDistanceMatrix(coordinates);
        }
    }

    @Override
    public RoadRoute getRoute(List<Coordinate> coordinates, RoutingMode mode) {
        try {
            String url = baseUrl(mode) + "/route/v1/driving/" + toCoordinatePath(coordinates)
                    + "?overview=full&geometries=geojson&steps=false";
            JsonNode response = getJson(url, "El proveedor no pudo calcular la geometría de la ruta.");
            ensureSuccess(response, "El proveedor no pudo calcular la geometría de la ruta.");
            JsonNode routes = response.path("routes");
            if (!routes.isArray() || routes.isEmpty()) {
                throw new TourismRoutingException("El proveedor no encontró una ruta vial para los destinos.");
            }
            JsonNode route = routes.get(0);
            JsonNode routeCoordinates = route.path("geometry").path("coordinates");
            if (!route.path("distance").isNumber() || !route.path("duration").isNumber()
                    || !routeCoordinates.isArray()) {
                throw new TourismRoutingException("El proveedor devolvió una geometría de ruta inválida.");
            }
            List<List<Double>> geometry = new ArrayList<>();
            for (JsonNode coordinate : routeCoordinates) {
                if (!coordinate.isArray() || coordinate.size() < 2
                        || !coordinate.get(0).isNumber() || !coordinate.get(1).isNumber()) {
                    throw new TourismRoutingException("El proveedor devolvió coordenadas de ruta inválidas.");
                }
                geometry.add(List.of(coordinate.get(0).asDouble(), coordinate.get(1).asDouble()));
            }
            return new RoadRoute(route.path("distance").asDouble(), route.path("duration").asDouble(), geometry);
        } catch (Exception ex) {
            return fallbackRoute(coordinates, mode);
        }
    }

    private double[][] fallbackDistanceMatrix(List<Coordinate> coordinates) {
        int n = coordinates.size();
        double[][] matrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    matrix[i][j] = 0;
                } else {
                    matrix[i][j] = haversineMeters(coordinates.get(i), coordinates.get(j));
                }
            }
        }
        return matrix;
    }

    private RoadRoute fallbackRoute(List<Coordinate> coordinates, RoutingMode mode) {
        double totalDistance = 0;
        List<List<Double>> geometry = new ArrayList<>();
        for (int i = 0; i < coordinates.size(); i++) {
            Coordinate c = coordinates.get(i);
            geometry.add(List.of(c.longitude(), c.latitude()));
            if (i > 0) {
                totalDistance += haversineMeters(coordinates.get(i - 1), c);
            }
        }
        double roadDistance = totalDistance * 1.3;
        double speedMps = mode == RoutingMode.WALKING ? 1.3 : 7.0;
        double durationSeconds = roadDistance / speedMps;
        return new RoadRoute(roadDistance, durationSeconds, geometry);
    }

    private double haversineMeters(Coordinate c1, Coordinate c2) {
        final double R = 6371000;
        double lat1 = Math.toRadians(c1.latitude());
        double lat2 = Math.toRadians(c2.latitude());
        double dLat = Math.toRadians(c2.latitude() - c1.latitude());
        double dLon = Math.toRadians(c2.longitude() - c1.longitude());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private JsonNode getJson(String url, String failureMessage) {
        try {
            return restClient.get().uri(URI.create(url)).retrieve().body(JsonNode.class);
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new TourismRoutingException(failureMessage, exception);
        }
    }

    private void ensureSuccess(JsonNode response, String failureMessage) {
        if (response == null || !"Ok".equals(response.path("code").asText())) {
            String providerMessage = response == null ? "" : response.path("message").asText();
            throw new TourismRoutingException(providerMessage.isBlank()
                    ? failureMessage
                    : failureMessage + " " + providerMessage);
        }
    }

    private String toCoordinatePath(List<Coordinate> coordinates) {
        if (coordinates == null || coordinates.size() < 2) {
            throw new IllegalArgumentException("Se requieren al menos dos coordenadas para solicitar una ruta.");
        }
        return coordinates.stream()
                .map(coordinate -> {
                    validateCoordinate(coordinate);
                    return coordinate.longitude() + "," + coordinate.latitude();
                })
                .reduce((first, second) -> first + ";" + second)
                .orElseThrow(() -> new IllegalArgumentException("No hay coordenadas para calcular la ruta."));
    }

    private void validateCoordinate(Coordinate coordinate) {
        if (coordinate == null
                || !Double.isFinite(coordinate.latitude()) || coordinate.latitude() < -90 || coordinate.latitude() > 90
                || !Double.isFinite(coordinate.longitude()) || coordinate.longitude() < -180
                || coordinate.longitude() > 180) {
            throw new IllegalArgumentException("Las coordenadas de la ruta son inválidas.");
        }
    }

    private String baseUrl(RoutingMode mode) {
        String url = mode == RoutingMode.WALKING ? walkingBaseUrl : drivingBaseUrl;
        if (url.isBlank()) {
            throw new TourismRoutingException("La URL del proveedor de rutas no está configurada.");
        }
        return url;
    }

    private String trimTrailingSlash(String value) {
        return value == null ? "" : value.replaceAll("/+$", "");
    }
}
