package com.example.demo.service;

import com.example.demo.entities.Room;
import com.example.demo.entities.RoomType;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.RoomTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.errors.NotFoundException;
import java.util.List;

@Service
public class RoomServiceImpl implements RoomService {

    @Autowired
    private RoomRepository roomRepo;

    @Autowired
    private RoomTypeRepository typeRepo;

    @Autowired
    private com.example.demo.repository.ReservationRoomRepository reservationRoomRepo;

    @Override
    public List<Room> findAll() {
        return roomRepo.findAll();
    }

    @Override
    public Room findById(Long id) {
        return roomRepo.findById(id).orElseThrow(() -> new NotFoundException(id));
    }

    @Override
    public List<Room> findByTypeId(Long typeId) {
        if (typeId == null) {
            return List.of();
        }
        return roomRepo.findByRoomType_Id(typeId);
    }

    @Override
    @Transactional
    public Room save(Room room) {
        if (room.getNumber() == null || room.getNumber().isBlank()) {
            throw new IllegalArgumentException("El número de habitación es obligatorio.");
        }

        // Asignar RoomType si viene typeId
        if (room.getTypeId() != null) {
            RoomType type = typeRepo.findById(room.getTypeId()).orElse(null);
            if (type != null) {
                room.setRoomType(type);
            }
        }

        if (room.getRoomType() == null) {
            List<RoomType> types = typeRepo.findAll();
            if (!types.isEmpty()) {
                room.setRoomType(types.get(0));
            } else {
                throw new IllegalArgumentException("Debes asignar un tipo de habitación válido.");
            }
        }

        // Validar número de habitación duplicado
        Room existing = roomRepo.findByNumber(room.getNumber().trim()).orElse(null);
        if (existing != null && !existing.getId().equals(room.getId())) {
            throw new IllegalArgumentException(
                    "Ya existe una habitación con el número '" + room.getNumber() + "'.");
        }

        return roomRepo.save(room);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Room room = roomRepo.findById(id).orElseThrow(() -> new NotFoundException(id));
        if (reservationRoomRepo.existsByRoom_Id(id)) {
            throw new IllegalArgumentException("No se puede eliminar la habitación porque tiene reservaciones registradas.");
        }
        roomRepo.delete(room);
    }
}
