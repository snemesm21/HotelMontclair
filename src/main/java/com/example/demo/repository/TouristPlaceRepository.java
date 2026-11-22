package com.example.demo.repository;

import com.example.demo.entities.TouristPlace;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TouristPlaceRepository extends JpaRepository<TouristPlace, Long> {
    List<TouristPlace> findByActiveTrueOrderByNameAsc();

    List<TouristPlace> findByActiveTrueAndCategoryIgnoreCaseOrderByNameAsc(String category);

    List<TouristPlace> findByActiveTrueOrderByDisplayOrderAscIdAsc();

    List<TouristPlace> findByActiveTrueAndCategoryIgnoreCaseOrderByDisplayOrderAscIdAsc(String category);

    Optional<TouristPlace> findBySlugAndActiveTrue(String slug);

    Optional<TouristPlace> findByIdAndActiveTrue(Long id);

    Optional<TouristPlace> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
