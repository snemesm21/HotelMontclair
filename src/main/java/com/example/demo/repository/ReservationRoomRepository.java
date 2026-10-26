package com.example.demo.repository;

import com.example.demo.entities.ReservationRoom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio Spring Data JPA para la entidad intermedia ReservationRoom.
 */
@Repository
public interface ReservationRoomRepository
        extends JpaRepository<ReservationRoom, Long> {

    boolean existsByRoom_Id(Long roomId);

    /**
     * Busca los IDs de habitaciones ocupadas en un rango de fechas.
     * Excluye reservas canceladas (CANCELLED) o finalizadas (INACTIVE),
     * garantizando que las habitaciones liberadas vuelvan a estar disponibles.
     */
    @Query("""
            SELECT DISTINCT rr.room.id
            FROM ReservationRoom rr
            JOIN rr.reservation r
            WHERE r.id <> :excludeReservationId
              AND (r.status IS NULL OR (UPPER(r.status) <> 'CANCELLED' AND UPPER(r.status) <> 'INACTIVE'))
              AND r.checkInDate < :checkOutDate
              AND r.checkOutDate > :checkInDate
            """)
    List<Long> findOccupiedRoomIds(
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("excludeReservationId") Long excludeReservationId);
}
