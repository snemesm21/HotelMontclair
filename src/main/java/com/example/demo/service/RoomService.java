package com.example.demo.service;

import com.example.demo.entities.Room;
import java.util.List;

public interface RoomService {
    List<Room> findAll();
    Room findById(Long id);
    Room save(Room room);
    void delete(Long id);
}
