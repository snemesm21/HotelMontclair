package com.example.demo.service;

import com.example.demo.entities.RoomType;
import com.example.demo.repository.RoomTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RoomTypeServiceImpl implements RoomTypeService {
    private final RoomTypeRepository repository;

    @Autowired
    public RoomTypeServiceImpl(RoomTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<RoomType> findAll() {
        return repository.findAll();
    }

    @Override
    public RoomType findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public RoomType save(RoomType roomType) {
        return repository.save(roomType);
    }

    @Override
    public void delete(Long id) {
        repository.delete(id);
    }
}

