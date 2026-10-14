
package com.example.demo.controller;

import com.example.demo.entities.Room;
import com.example.demo.entities.RoomStatus;
import com.example.demo.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "http://localhost:4200")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @Operation(summary = "Obtiene todas las habitaciones, opcionalmente filtradas por tipo o estado")
    @GetMapping
    public List<Room> getAllRooms(
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) RoomStatus status) {

        if (typeId != null && status != null) {
            return roomService.findByTypeId(typeId)
                    .stream()
                    .filter(room -> room.getStatus() == status)
                    .toList();
        }

        if (typeId != null) {
            return roomService.findByTypeId(typeId);
        }

        if (status != null) {
            return roomService.findByStatus(status);
        }

        return roomService.findAll();
    }

    @Operation(summary = "Obtiene todas las habitaciones que pertenecen a un tipo de habitación")
    @GetMapping({ "/type/{typeId}", "/type-room/{typeId}", "/room-type/{typeId}" })
    public List<Room> getRoomsByTypeId(@PathVariable Long typeId) {
        return roomService.findByTypeId(typeId);
    }

    @Operation(summary = "Obtiene un cuarto por su ID")
    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable Long id) {
        Room room = roomService.findById(id);

        if (room == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(room);
    }

    @Operation(summary = "Crea un nuevo cuarto")
    @PostMapping
    public ResponseEntity<Room> createRoom(@RequestBody Room room) {
        room.setId(null);
        Room saved = roomService.save(room);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Actualiza un cuarto existente por su ID")
    @PutMapping("/{id}")
    public ResponseEntity<Room> updateRoom(
            @PathVariable Long id,
            @RequestBody Room room) {

        room.setId(id);
        Room updated = roomService.save(room);

        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Elimina un cuarto por su ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
