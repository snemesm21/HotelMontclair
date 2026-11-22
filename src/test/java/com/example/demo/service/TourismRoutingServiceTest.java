package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.demo.dto.OptimizedRouteResponse;
import com.example.demo.dto.RouteOptimizationRequest;
import com.example.demo.entities.ItineraryStop;
import com.example.demo.entities.TouristPlace;
import java.util.List;
import org.junit.jupiter.api.Test;

class TourismRoutingServiceTest {

    @Test
    void optimizesStopsUsingRoadMatrixAndReturnsRoadGeometry() {
        TourismService tourismService = mock(TourismService.class);
        RoutingProvider provider = mock(RoutingProvider.class);
        List<ItineraryStop> stops = List.of(
                stop(11L, "A"), stop(12L, "B"), stop(13L, "C"));
        when(tourismService.getItineraryEntities(7L)).thenReturn(stops);
        when(provider.getDistanceMatrix(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.eq(RoutingMode.DRIVING)))
                .thenReturn(new double[][] {
                        {0, 1, 2, 9},
                        {1, 0, 8, 2},
                        {2, 8, 0, 1},
                        {9, 2, 1, 0}
                });
        when(provider.getRoute(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.eq(RoutingMode.DRIVING)))
                .thenReturn(new RoutingProvider.RoadRoute(
                        400, 180, List.of(List.of(7.42, 43.73), List.of(7.43, 43.74))));

        TourismRoutingService service = new TourismRoutingService(
                tourismService, provider, "Hotel, Monaco", "43.73", "7.42");
        OptimizedRouteResponse response = service.optimize(7L, new RouteOptimizationRequest("DRIVING", false));

        assertEquals(List.of("A", "C", "B"), response.visits().stream()
                .filter(point -> "DESTINATION".equals(point.type()))
                .map(OptimizedRouteResponse.RoutePoint::name)
                .toList());
        assertEquals(400, response.distanceMeters());
        assertEquals(180, response.durationSeconds());
        assertEquals("LineString", response.geometryType());
        assertEquals(2, response.geometry().size());
        assertEquals("driving", response.googleMapsUrl().substring(response.googleMapsUrl().lastIndexOf("travelmode=") + 11));
    }

    @Test
    void rejectsEmptyItineraryBeforeCallingRoutingProvider() {
        TourismService tourismService = mock(TourismService.class);
        when(tourismService.getItineraryEntities(7L)).thenReturn(List.of());
        TourismRoutingService service = new TourismRoutingService(
                tourismService, mock(RoutingProvider.class), "Hotel, Monaco", "43.73", "7.42");

        assertThrows(IllegalArgumentException.class,
                () -> service.optimize(7L, new RouteOptimizationRequest("WALKING", true)));
    }

    @Test
    void requiresVerifiedHotelCoordinates() {
        TourismService tourismService = mock(TourismService.class);
        when(tourismService.getItineraryEntities(7L)).thenReturn(List.of(stop(11L, "A")));
        TourismRoutingService service = new TourismRoutingService(
                tourismService, mock(RoutingProvider.class), "Hotel, Monaco", "", "");

        assertThrows(TourismRoutingConfigurationException.class,
                () -> service.optimize(7L, new RouteOptimizationRequest("WALKING", false)));
    }

    @Test
    void rejectsInvalidDestinationCoordinatesBeforeCallingProvider() {
        TourismService tourismService = mock(TourismService.class);
        RoutingProvider provider = mock(RoutingProvider.class);
        TouristPlace place = stop(11L, "A").getPlace();
        place.setLatitude(91);
        when(tourismService.getItineraryEntities(7L)).thenReturn(List.of(stop(place)));
        TourismRoutingService service = new TourismRoutingService(
                tourismService, provider, "Hotel, Monaco", "43.73", "7.42");

        assertThrows(IllegalArgumentException.class,
                () -> service.optimize(7L, new RouteOptimizationRequest("WALKING", false)));
        verifyNoInteractions(provider);
    }

    @Test
    void surfacesRoutingProviderFailures() {
        TourismService tourismService = mock(TourismService.class);
        RoutingProvider provider = mock(RoutingProvider.class);
        when(tourismService.getItineraryEntities(7L)).thenReturn(List.of(stop(11L, "A")));
        when(provider.getDistanceMatrix(org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.eq(RoutingMode.WALKING)))
                .thenThrow(new TourismRoutingException("OSRM no disponible."));
        TourismRoutingService service = new TourismRoutingService(
                tourismService, provider, "Hotel, Monaco", "43.73", "7.42");

        TourismRoutingException exception = assertThrows(TourismRoutingException.class,
                () -> service.optimize(7L, new RouteOptimizationRequest("WALKING", false)));
        assertEquals("OSRM no disponible.", exception.getMessage());
    }

    @Test
    void includesHotelAtBothEndsWhenReturnIsRequested() {
        TourismService tourismService = mock(TourismService.class);
        RoutingProvider provider = mock(RoutingProvider.class);
        when(tourismService.getItineraryEntities(7L)).thenReturn(List.of(stop(11L, "A")));
        when(provider.getDistanceMatrix(org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.eq(RoutingMode.DRIVING)))
                .thenReturn(new double[][] {{0, 5, 0}, {5, 0, 5}, {0, 5, 0}});
        when(provider.getRoute(org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.eq(RoutingMode.DRIVING)))
                .thenReturn(new RoutingProvider.RoadRoute(10, 5, List.of(List.of(7.42, 43.73))));
        TourismRoutingService service = new TourismRoutingService(
                tourismService, provider, "Hotel, Monaco", "43.73", "7.42");

        OptimizedRouteResponse response = service.optimize(7L, new RouteOptimizationRequest("DRIVING", true));

        assertEquals(true, response.returnToHotel());
        assertEquals("HOTEL", response.visits().get(0).type());
        assertEquals("HOTEL", response.visits().get(2).type());
        assertEquals("DESTINATION", response.visits().get(1).type());
    }

    private ItineraryStop stop(Long id, String name) {
        TouristPlace place = new TouristPlace();
        place.setId(id);
        place.setName(name);
        place.setLatitude(43.73);
        place.setLongitude(7.42);
        place.setSuggestedDurationMinutes(30);
        ItineraryStop stop = new ItineraryStop();
        stop.setPlace(place);
        return stop;
    }

    private ItineraryStop stop(TouristPlace place) {
        ItineraryStop stop = new ItineraryStop();
        stop.setPlace(place);
        return stop;
    }
}
