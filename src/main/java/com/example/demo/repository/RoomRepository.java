package com.example.demo.repository;

import com.example.demo.entities.Room;
import com.example.demo.entities.RoomStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByNumber(String number);

    boolean existsByRoomType_Id(Long roomTypeId);

    List<Room> findByRoomType_Id(Long roomTypeId);

    List<Room> findByStatus(RoomStatus status);

    // Consulta de habitaciones que cumplen los requisitos básicos de tipo, estado y capacidad
    List<Room> findByRoomType_IdAndStatusAndCapacityGreaterThanEqualOrderByIdAsc(
            Long roomTypeId,
            RoomStatus status,
            int numberOfPeople);
}
