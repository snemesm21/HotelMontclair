package com.example.demo.service;

import com.example.demo.entities.Room;
import com.example.demo.entities.RoomType;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.RoomTypeRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RoomServiceImpl implements RoomService {
    private final RoomRepository roomRepo;
    private final RoomTypeRepository typeRepo;

    public RoomServiceImpl(RoomRepository roomRepo, RoomTypeRepository typeRepo) {
        this.roomRepo = roomRepo;
        this.typeRepo = typeRepo;
    }

    @Override
    public List<Room> findAll() {
        return roomRepo.findAll();
    }

    @Override
    public Room findById(Long id) {
        return roomRepo.findById(id);
    }

    @Override
    public Room save(Room room) {
        if (room.getTypeId() != null && typeRepo.findById(room.getTypeId()) == null) {
            throw new IllegalArgumentException("RoomType with id " + room.getTypeId() + " not found");
        }
        return roomRepo.save(room);
    }

    @Override
    public void delete(Long id) {
        roomRepo.delete(id);
    }
}
