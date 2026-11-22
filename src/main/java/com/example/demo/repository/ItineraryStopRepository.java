package com.example.demo.repository;

import com.example.demo.entities.ItineraryStop;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryStopRepository extends JpaRepository<ItineraryStop, Long> {
    List<ItineraryStop> findAllByClientIdOrderByAddedAtAsc(Long clientId);

    Optional<ItineraryStop> findByClientIdAndPlaceId(Long clientId, Long placeId);

    boolean existsByClientIdAndPlaceId(Long clientId, Long placeId);

    void deleteByClientIdAndPlaceId(Long clientId, Long placeId);
}
