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

    @Override
    public List<Room> findAll() {
        return roomRepo.findAll();
    }

    @Override
    public Room findById(Long id) {
        Room room = roomRepo.findById(id).orElse(null);
        if (room == null) {
            throw new NotFoundException(id);
        }
        return room;
    }

    @Override
    @Transactional
    public Room save(Room room) {
        if (room.getNumber() == null || room.getNumber().isBlank()) {
            throw new IllegalArgumentException("El número de habitación es obligatorio.");
        }
        if (room.getTypeId() != null) {
            RoomType type = typeRepo.findById(room.getTypeId()).orElse(null);
            if (type != null) {
                room.setType(type);
            }
        } else if (room.getType() == null) {
            List<RoomType> types = typeRepo.findAll();
            if (!types.isEmpty()) {
                room.setType(types.get(0));
            }
        }
        // Validar que tenga tipo asignado
        if (room.getType() == null) {
            throw new IllegalArgumentException(
                    "Debes seleccionar un tipo de habitación. Asegúrate de haber creado al menos un tipo antes.");
        }

        if (room.getId() != null) {
            Room existing = roomRepo.findById(room.getId()).orElse(null);
            if (existing != null) {
                if (room.getHighlights() == null || room.getHighlights().isEmpty()) {
                    room.setHighlights(existing.getHighlights());
                }
                if (room.getGalleryImages() == null || room.getGalleryImages().isEmpty()) {
                    room.setGalleryImages(existing.getGalleryImages());
                }
                if (room.getHeroDescription() == null || room.getHeroDescription().isBlank()) {
                    room.setHeroDescription(existing.getHeroDescription());
                }
                if (room.getHeadline() == null || room.getHeadline().isBlank()) {
                    room.setHeadline(existing.getHeadline());
                }
                if (room.getFullDescription() == null || room.getFullDescription().isBlank()) {
                    room.setFullDescription(existing.getFullDescription());
                }
            }
        }
        // Validar número de habitación duplicado
        if (room.getNumber() != null && !room.getNumber().isBlank()) {
            Room existing = roomRepo.findByNumber(room.getNumber()).orElse(null);
            if (existing != null && !existing.getId().equals(room.getId())) {
                throw new IllegalArgumentException(
                        "Ya existe una habitación con el número '" + room.getNumber()
                                + "'. Por favor elige un número diferente.");
            }
        }
        return roomRepo.save(room);

    }

    @Override
    @Transactional
    public void delete(Long id) {
        Room room = roomRepo.findById(id).orElse(null);
        if (room == null) {
            throw new NotFoundException(id);
        }
        roomRepo.deleteById(id);
    }
}
