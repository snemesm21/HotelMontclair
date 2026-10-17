
package com.example.demo.repository;

import com.example.demo.entities.Room;
import com.example.demo.entities.RoomStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByNumber(String number);

    boolean existsByRoomType_Id(Long roomTypeId);

    List<Room> findByRoomType_Id(Long roomTypeId);

    List<Room> findByStatus(RoomStatus status);

    @Query("""
            SELECT room
            FROM Room room
            WHERE room.roomType.id = :roomTypeId
              AND room.status = :status
              AND room.capacity >= :numberOfPeople
              AND NOT EXISTS (
                  SELECT rr.id
                  FROM ReservationRoom rr
                  JOIN rr.reservation reservation
                  WHERE rr.room = room
                    AND reservation.id <> :excludeReservationId
                    AND reservation.checkInDate < :checkOutDate
                    AND reservation.checkOutDate > :checkInDate
              )
            ORDER BY room.id
            """)
    List<Room> findAvailableRooms(
            @Param("roomTypeId") Long roomTypeId,
            @Param("status") RoomStatus status,
            @Param("numberOfPeople") int numberOfPeople,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("excludeReservationId") Long excludeReservationId);

    default boolean existsByType_Id(Long id) {
        return existsByRoomType_Id(id);
    }

    default List<Room> findByType_Id(Long typeId) {
        return findByRoomType_Id(typeId);
    }
}
