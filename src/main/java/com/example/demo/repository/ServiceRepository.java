package com.example.demo.repository;

import com.example.demo.entities.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {
    Optional<Service> findByTag(String tag);
    List<Service> findByHidden(boolean hidden);
}