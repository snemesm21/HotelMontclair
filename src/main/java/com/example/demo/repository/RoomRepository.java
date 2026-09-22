
package com.example.demo.repository;

import com.example.demo.entities.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByNumber(String number);
    List<Room> findByStatus(String status);
}
