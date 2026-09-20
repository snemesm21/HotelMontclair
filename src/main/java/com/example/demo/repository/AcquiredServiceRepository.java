package com.example.demo.repository;

import com.example.demo.entities.AcquiredService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AcquiredServiceRepository extends JpaRepository<AcquiredService, Long> {
}
