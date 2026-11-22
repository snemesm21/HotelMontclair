package com.example.demo.repository;

import com.example.demo.entities.TouristPlace;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TouristPlaceRepository extends JpaRepository<TouristPlace, Long> {
    List<TouristPlace> findByActiveTrueOrderByNameAsc();

    List<TouristPlace> findByActiveTrueAndCategoryIgnoreCaseOrderByNameAsc(String category);

    Optional<TouristPlace> findBySlugAndActiveTrue(String slug);

    boolean existsBySlug(String slug);
}
