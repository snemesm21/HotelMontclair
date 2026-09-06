package com.example.demo.service;

import com.example.demo.entities.RoomType;
import com.example.demo.entities.Room;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.RoomTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.errors.NotFoundException;
import java.util.List;

@Service
public class RoomTypeServiceImpl implements RoomTypeService {

    @Autowired
    private RoomTypeRepository repository;

    @Autowired
    private RoomRepository roomRepository;

    @Override
    public List<RoomType> findAll() {
        return repository.findAll();
    }

    @Override
    public RoomType findById(Long id) {
        RoomType roomType = repository.findById(id).orElse(null);
        if (roomType == null) {
            throw new NotFoundException(id);
        }
        return roomType;
    }

    @Override
    @Transactional
    public RoomType save(RoomType roomType) {
        RoomType savedType = repository.save(roomType);
        for (Room room : roomRepository.findAll()) {
            if (room.getType() != null && savedType.getId().equals(room.getType().getId())) {
                room.setPricePerNight(savedType.getPricePerNight());
                roomRepository.save(room);
            }
        }
        return savedType;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        RoomType roomType = repository.findById(id).orElse(null);
        if (roomType == null) {
            throw new NotFoundException(id);
        }
        repository.deleteById(id);
    }
}

