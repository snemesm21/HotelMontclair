package com.example.demo.service;

import com.example.demo.entities.Room;
import java.util.Collections;
import java.util.List;

public interface RoomService {
    List<Room> findAll();
    Room findById(Long id);
    default List<Room> findByTypeId(Long typeId) {
        return Collections.emptyList();
    }
    Room save(Room room);
    void delete(Long id);
}
