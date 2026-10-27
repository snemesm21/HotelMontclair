package com.example.demo.repository;

import com.example.demo.entities.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad Reservation.
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByClientId(Long clientId);
    List<Reservation> findByClientId(Long clientId);

    /**
     * Busca las reservas con estado ACTIVE asociadas a una habitacion especifica.
     * Utilizado para validar la Regla de Negocio 5:
     * 'En caso de que en la habitacion no haya ninguna reserva activa, no se debe poder asignar ningun servicio'.
     */
    @Query("""
            SELECT DISTINCT r FROM Reservation r
            JOIN r.reservationRooms rr
            WHERE rr.room.id = :roomId
              AND UPPER(r.status) = 'ACTIVE'
            """)
    List<Reservation> findActiveReservationsByRoomId(@Param("roomId") Long roomId);

    /**
     * Busca la unica reserva con estado ACTIVE asociada al numero de habitacion (e.g. "101", "205").
     * Regla de negocio: Una habitacion fisica solo puede tener a lo sumo UNA reserva activa al mismo tiempo.
     */
    @Query("""
            SELECT DISTINCT r FROM Reservation r
            JOIN r.reservationRooms rr
            WHERE UPPER(TRIM(rr.room.number)) = UPPER(TRIM(:roomNumber))
              AND UPPER(r.status) = 'ACTIVE'
            """)
    Optional<Reservation> findActiveReservationByRoomNumber(@Param("roomNumber") String roomNumber);
}
