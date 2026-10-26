
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

//Controlador REST de habitaciones (/api/rooms).
@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "http://localhost:4200")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private com.example.demo.service.ReservationManagerService reservationService;

    //Lista habitaciones con filtros opcionales (GET /api/rooms). Combinaciones posibles: sin parámetros (todas), solo `typeId`, solo `status`, o ambos a la vez.
    @Operation(summary = "Obtiene todas las habitaciones, opcionalmente filtradas por tipo o estado")
    @GetMapping
    public List<Room> getAllRooms(
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) RoomStatus status) {
        //Con los dos filtros: se busca por tipo y el estado se filtra en memoria.
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

    //Lista las habitaciones de un tipo (GET /api/rooms/type/{typeId}).
    @Operation(summary = "Obtiene todas las habitaciones que pertenecen a un tipo de habitación")
    @GetMapping({ "/type/{typeId}", "/type-room/{typeId}", "/room-type/{typeId}" })
    public List<Room> getRoomsByTypeId(@PathVariable Long typeId) {
        return roomService.findByTypeId(typeId);
    }

    //Busca una habitación por id (GET /api/rooms/{id}).
    @Operation(summary = "Obtiene un cuarto por su ID")
    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable Long id) {
        Room room = roomService.findById(id);

        if (room == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(room);
    }

    //Crea una habitación (POST /api/rooms). Se pone el id en null para que la base de datos genere uno nuevo.
    @Operation(summary = "Crea un nuevo cuarto")
    @PostMapping
    public ResponseEntity<Room> createRoom(@RequestBody Room room) {
        room.setId(null);
        Room saved = roomService.save(room);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    //Actualiza una habitación (PUT /api/rooms/{id}). El id de la URL manda sobre el del cuerpo.
    @Operation(summary = "Actualiza un cuarto existente por su ID")
    @PutMapping("/{id}")
    public ResponseEntity<Room> updateRoom(
            @PathVariable Long id,
            @RequestBody Room room) {

        room.setId(id);
        Room updated = roomService.save(room);

        return ResponseEntity.ok(updated);
    }

    //Elimina una habitación (DELETE /api/rooms/{id}).
    @Operation(summary = "Elimina un cuarto por su ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.delete(id);
        return ResponseEntity.noContent().build();
    }
    /**
     * Asigna un servicio consumido a una habitacion (POST /api/rooms/{roomId}/services).
     * REGLA DE NEGOCIO 5: En caso de que en la habitacion no haya ninguna reserva activa,
     * no se debe poder asignar ningun servicio.
     */
    @Operation(summary = "Asigna un servicio consumido a una habitacion (requiere reserva activa)")
    @PostMapping("/{roomId}/services")
    public ResponseEntity<?> assignServiceToRoom(
            @PathVariable Long roomId,
            @RequestBody java.util.Map<String, Object> body) {
        try {
            if (!body.containsKey("serviceId")) {
                return ResponseEntity.badRequest().body(java.util.Map.of("message", "El serviceId es obligatorio."));
            }
            Long serviceId = Long.valueOf(body.get("serviceId").toString());
            int quantity = body.containsKey("quantity") ? Integer.parseInt(body.get("quantity").toString()) : 1;

            com.example.demo.dto.ReservationDTO updated = reservationService.assignServiceToRoom(roomId, serviceId, quantity);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("message", "Error al asignar servicio: " + e.getMessage()));
        }
    }
}