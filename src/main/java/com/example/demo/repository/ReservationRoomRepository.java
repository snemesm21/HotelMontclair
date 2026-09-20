package com.example.demo.repository;

import com.example.demo.entities.ReservationRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRoomRepository extends JpaRepository<ReservationRoom, Long> {
}
