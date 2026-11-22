package com.example.demo.service;

import java.util.List;

public interface RoutingProvider {

    double[][] getDistanceMatrix(List<Coordinate> coordinates, RoutingMode mode);

    RoadRoute getRoute(List<Coordinate> coordinates, RoutingMode mode);

    record Coordinate(double latitude, double longitude) {
    }

    record RoadRoute(double distanceMeters, double durationSeconds, List<List<Double>> geometry) {
    }
}
