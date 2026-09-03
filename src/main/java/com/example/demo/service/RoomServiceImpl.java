package com.example.demo.service;

import com.example.demo.entities.Room;
import com.example.demo.entities.RoomType;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.RoomTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
        return roomRepo.findById(id);
    }

    @Override
    public Room save(Room room) {
        if (room.getTypeId() != null && typeRepo.findById(room.getTypeId()) == null) {
            List<RoomType> types = typeRepo.findAll();
            if (!types.isEmpty()) {
                room.setTypeId(types.get(0).getId());
            }
        }
        if (room.getImageUrl() == null || room.getImageUrl().isBlank()) {
            room.setImageUrl("https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=800&q=80");
        }
        if (room.getId() != null) {
            Room existing = roomRepo.findById(room.getId());
            if (existing != null) {
                if (room.getHighlights() == null || room.getHighlights().isEmpty()) {
                    room.setHighlights(existing.getHighlights());
                }
                if (room.getGalleryImages() == null || room.getGalleryImages().isEmpty()) {
                    room.setGalleryImages(existing.getGalleryImages());
                }
                if (room.getSecondaryImageUrl() == null || room.getSecondaryImageUrl().isBlank()) {
                    room.setSecondaryImageUrl(existing.getSecondaryImageUrl());
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
        return roomRepo.save(room);
    }

    @Override
    public void delete(Long id) {
        roomRepo.delete(id);
    }
}
