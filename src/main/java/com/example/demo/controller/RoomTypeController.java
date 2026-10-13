package com.example.demo.controller;

import com.example.demo.entities.RoomType;
import com.example.demo.service.RoomTypeService;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping({"/api/room-types"})
@CrossOrigin(origins = "http://localhost:4200")
public class RoomTypeController {

    @Autowired
    private RoomTypeService roomTypeService;

    @Operation (summary = "Obtiene todos los tipos de cuarto")
    @GetMapping
    public List<RoomType> getAllRoomTypes() {
        return roomTypeService.findAll();
    }

    @Operation (summary = "Obtiene un tipo de cuarto por su ID")
    @GetMapping("/{id}")
    public ResponseEntity<RoomType> getRoomTypeById(@PathVariable Long id) {
        RoomType rt = roomTypeService.findById(id);
        if (rt == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rt);
    }

    @Operation (summary = "Crea un nuevo tipo de cuarto")
    @PostMapping
    public ResponseEntity<RoomType> createRoomType(@RequestBody RoomType roomType) {
        roomType.setId(null);
        RoomType saved = roomTypeService.save(roomType);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation (summary = "Actualiza un tipo de cuarto existente por su ID")
    @PutMapping("/{id}")
    public ResponseEntity<RoomType> updateRoomType(@PathVariable Long id, @RequestBody RoomType roomType) {
        roomType.setId(id);
        RoomType updated = roomTypeService.save(roomType);
        return ResponseEntity.ok(updated);
    }

    @Operation (summary = "Elimina un tipo de cuarto por su ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoomType(@PathVariable Long id) {
        roomTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
