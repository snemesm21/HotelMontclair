package com.example.demo.repository;

import com.example.demo.entities.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByClientId(Long clientId);

    @Query("SELECT r FROM Reservation r WHERE r.client.id = :clientId")
    List<Reservation> findByClientId(@Param("clientId") Long clientId);

    @Query("SELECT r FROM Reservation r WHERE r.status = :status")
    List<Reservation> findByStatus(@Param("status") String status);

    @Query("SELECT r FROM Reservation r WHERE r.checkInDate BETWEEN :startDate AND :endDate")
    List<Reservation> findByCheckInDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT r FROM Reservation r WHERE r.checkOutDate BETWEEN :startDate AND :endDate")
    List<Reservation> findByCheckOutDateBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT r FROM Reservation r WHERE r.client.id = :clientId AND r.status = :status")
    List<Reservation> findByClientIdAndStatus(
            @Param("clientId") Long clientId,
            @Param("status") String status);

    
    @Query("""
            SELECT DISTINCT r FROM Reservation r
            JOIN r.reservationRooms rr
            WHERE rr.room.id = :roomId
              AND UPPER(r.status) = 'ACTIVE'
            """)
    List<Reservation> findActiveReservationsByRoomId(@Param("roomId") Long roomId);

    
    @Query("""
            SELECT DISTINCT r FROM Reservation r
            JOIN r.reservationRooms rr
            WHERE UPPER(TRIM(rr.room.number)) = UPPER(TRIM(:roomNumber))
              AND UPPER(r.status) = 'ACTIVE'
            """)
    Optional<Reservation> findActiveReservationByRoomNumber(
            @Param("roomNumber") String roomNumber);
}