package com.example.demo.repository;

import com.example.demo.entities.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByClientId(Long clientId);
    List<Reservation> findByClientId(Long clientId);
}
