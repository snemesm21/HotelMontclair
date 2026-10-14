package com.example.demo.repository;

import com.example.demo.entities.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByNumber(String number);
    boolean existsByRoomType_Id(Long roomTypeId);
    List<Room> findByRoomType_Id(Long roomTypeId);

    default boolean existsByType_Id(Long id) {
        return existsByRoomType_Id(id);
    }

    default List<Room> findByType_Id(Long typeId) {
        return findByRoomType_Id(typeId);
    }
}
