
package com.example.demo.service;

import com.example.demo.entities.Room;
import com.example.demo.entities.RoomStatus;

import java.util.List;

public interface RoomService {

    List<Room> findAll();

    Room findById(Long id);

    List<Room> findByTypeId(Long typeId);

    List<Room> findByStatus(RoomStatus status);

    Room save(Room room);

    void delete(Long id);
}
