package com.example.demo.service;

import com.example.demo.entities.RoomType;
import java.util.List;

public interface RoomTypeService {
    List<RoomType> findAll();
    RoomType findById(Long id);
    RoomType save(RoomType roomType);
    void delete(Long id);
}

