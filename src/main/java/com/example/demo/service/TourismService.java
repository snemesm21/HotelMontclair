package com.example.demo.service;

import com.example.demo.dto.ItineraryStopResponse;
import com.example.demo.dto.TouristPlaceRequest;
import com.example.demo.dto.TouristPlaceResponse;
import com.example.demo.entities.Client;
import com.example.demo.entities.ItineraryStop;
import com.example.demo.entities.TouristPlace;
import com.example.demo.errors.NotFoundException;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.ItineraryStopRepository;
import com.example.demo.repository.TouristPlaceRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TourismService {

    private final TouristPlaceRepository placeRepository;
    private final ItineraryStopRepository itineraryRepository;
    private final ClientRepository clientRepository;
    private final String hotelAddress;

    public TourismService(
            TouristPlaceRepository placeRepository,
            ItineraryStopRepository itineraryRepository,
            ClientRepository clientRepository,
            @Value("${hotelmontclair.tourism.hotel.address:Montclair Grand Hotel - L'Hermitage, 98000 Monaco}")
            String hotelAddress) {
        this.placeRepository = placeRepository;
        this.itineraryRepository = itineraryRepository;
        this.clientRepository = clientRepository;
        this.hotelAddress = hotelAddress;
    }

    @Transactional(readOnly = true)
    public List<TouristPlaceResponse> getPlaces(String category) {
        List<TouristPlace> places = category == null || category.isBlank()
                ? placeRepository.findByActiveTrueOrderByNameAsc()
                : placeRepository.findByActiveTrueAndCategoryIgnoreCaseOrderByNameAsc(category.trim());
        return places.stream().map(this::toPlaceResponse).toList();
    }

    @Transactional(readOnly = true)
    public TouristPlaceResponse getPlace(String slug) {
        return placeRepository.findBySlugAndActiveTrue(slug)
                .map(this::toPlaceResponse)
                .orElseThrow(() -> new NotFoundException("No se encontró el destino turístico solicitado."));
    }

    @Transactional
    public TouristPlaceResponse createPlace(TouristPlaceRequest request) {
        validatePlace(request);
        String slug = request.slug().trim().toLowerCase(Locale.ROOT);
        if (placeRepository.existsBySlug(slug)) {
            throw new IllegalStateException("Ya existe un destino con ese slug.");
        }
        TouristPlace place = new TouristPlace();
        place.setSlug(slug);
        place.setName(request.name().trim());
        place.setDescription(request.description().trim());
        place.setCategory(request.category().trim().toUpperCase(Locale.ROOT));
        place.setImageUrls(request.imageUrls().stream().map(String::trim).toList());
        place.setAddress(request.address().trim());
        place.setLatitude(request.latitude());
        place.setLongitude(request.longitude());
        place.setSuggestedDurationMinutes(request.suggestedDurationMinutes());
        place.setActive(true);
        return toPlaceResponse(placeRepository.save(place));
    }

    @Transactional(readOnly = true)
    public List<ItineraryStopResponse> getItinerary(Long clientId) {
        return itineraryRepository.findAllByClientIdOrderByAddedAtAsc(clientId).stream()
                .map(this::toStopResponse)
                .toList();
    }

    @Transactional
    public ItineraryStopResponse addToItinerary(Long clientId, Long placeId) {
        TouristPlace place = placeRepository.findById(placeId)
                .filter(TouristPlace::isActive)
                .orElseThrow(() -> new NotFoundException("No se encontró el destino turístico solicitado."));
        return itineraryRepository.findByClientIdAndPlaceId(clientId, placeId)
                .map(this::toStopResponse)
                .orElseGet(() -> {
                    Client client = clientRepository.findById(clientId)
                            .orElseThrow(() -> new NotFoundException("No se encontró el huésped autenticado."));
                    try {
                        return toStopResponse(itineraryRepository.saveAndFlush(new ItineraryStop(client, place)));
                    } catch (DataIntegrityViolationException exception) {
                        return itineraryRepository.findByClientIdAndPlaceId(clientId, placeId)
                                .map(this::toStopResponse)
                                .orElseThrow(() -> exception);
                    }
                });
    }

    @Transactional
    public void removeFromItinerary(Long clientId, Long placeId) {
        if (!itineraryRepository.existsByClientIdAndPlaceId(clientId, placeId)) {
            throw new NotFoundException("El destino no está en el itinerario.");
        }
        itineraryRepository.deleteByClientIdAndPlaceId(clientId, placeId);
    }

    @Transactional(readOnly = true)
    public List<ItineraryStop> getItineraryEntities(Long clientId) {
        return itineraryRepository.findAllByClientIdOrderByAddedAtAsc(clientId);
    }

    public TouristPlaceResponse toPlaceResponse(TouristPlace place) {
        String destination = place.getLatitude() + "," + place.getLongitude();
        String url = "https://www.google.com/maps/dir/?api=1&origin=" + encode(hotelAddress)
                + "&destination=" + encode(destination);
        return new TouristPlaceResponse(
                place.getId(),
                place.getSlug(),
                place.getName(),
                place.getDescription(),
                place.getCategory(),
                List.copyOf(place.getImageUrls()),
                place.getAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getSuggestedDurationMinutes(),
                url);
    }

    private ItineraryStopResponse toStopResponse(ItineraryStop stop) {
        return new ItineraryStopResponse(stop.getId(), toPlaceResponse(stop.getPlace()), stop.getAddedAt());
    }

    private void validatePlace(TouristPlaceRequest request) {
        if (request == null
                || isBlank(request.slug()) || isBlank(request.name()) || isBlank(request.description())
                || isBlank(request.category()) || request.imageUrls() == null || request.imageUrls().isEmpty()
                || request.imageUrls().stream().anyMatch(TourismService::isBlank)
                || isBlank(request.address()) || request.latitude() == null || request.longitude() == null
                || request.suggestedDurationMinutes() == null || request.suggestedDurationMinutes() <= 0) {
            throw new IllegalArgumentException("El destino requiere slug, nombre, descripción, categoría, imágenes, dirección, coordenadas y duración válida.");
        }
        validateCoordinate(request.latitude(), request.longitude());
    }

    static void validateCoordinate(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Las coordenadas del destino son inválidas.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
