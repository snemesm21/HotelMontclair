package com.example.demo.controller;

import com.example.demo.entities.Reservation;
import com.example.demo.service.ReservationManagerService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "http://localhost:4200")
public class ReservationController {

    @Autowired
    private ReservationManagerService reservationService;

    @Operation(summary = "Obtiene todas las reservas")
    @GetMapping
    public List<Reservation> getAllReservations() {
        return reservationService.searchAll();
    }

    @Operation(summary = "Obtiene una reserva por su ID")
    @GetMapping("/{id}")
    public ResponseEntity<Reservation> getReservationById(@PathVariable Long id) {
        Reservation res = reservationService.searchById(id);
        if (res == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(res);
    }

    @Operation(summary = "Obtiene las reservas de un cliente por su ID")
    @GetMapping("/client/{clientId}")
    public List<Reservation> getReservationsByClientId(@PathVariable Long clientId) {
        return reservationService.searchByClientId(clientId);
    }

    @Operation(summary = "Crea una nueva reserva")
    @PostMapping
    public ResponseEntity<?> createReservation(@RequestBody Map<String, Object> req) {
        try {
            Long clientId = req.get("clientId") != null ? Long.valueOf(req.get("clientId").toString()) : null;
            Long roomId = req.get("roomId") != null ? Long.valueOf(req.get("roomId").toString()) : null;
            LocalDate checkIn = req.get("checkInDate") != null ? LocalDate.parse(req.get("checkInDate").toString()) : null;
            LocalDate checkOut = req.get("checkOutDate") != null ? LocalDate.parse(req.get("checkOutDate").toString()) : null;
            int people = req.get("numberOfPeople") != null ? Integer.parseInt(req.get("numberOfPeople").toString()) : 1;

            List<Long> serviceIds = null;
            if (req.get("serviceIds") instanceof List<?> rawList) {
                serviceIds = rawList.stream().map(s -> Long.valueOf(s.toString())).toList();
            }

            Reservation saved = reservationService.createReservation(clientId, roomId, checkIn, checkOut, people, serviceIds);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al crear reserva: " + e.getMessage());
        }
    }

    @Operation(summary = "Actualiza una reserva existente por su ID")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateReservation(@PathVariable Long id, @RequestBody Map<String, Object> req) {
        try {
            Long clientId = req.get("clientId") != null ? Long.valueOf(req.get("clientId").toString()) : null;
            Long roomId = req.get("roomId") != null ? Long.valueOf(req.get("roomId").toString()) : null;
            LocalDate checkIn = req.get("checkInDate") != null ? LocalDate.parse(req.get("checkInDate").toString()) : null;
            LocalDate checkOut = req.get("checkOutDate") != null ? LocalDate.parse(req.get("checkOutDate").toString()) : null;
            int people = req.get("numberOfPeople") != null ? Integer.parseInt(req.get("numberOfPeople").toString()) : 1;

            List<Long> serviceIds = null;
            if (req.get("serviceIds") instanceof List<?> rawList) {
                serviceIds = rawList.stream().map(s -> Long.valueOf(s.toString())).toList();
            }

            Reservation updated = reservationService.updateReservation(id, clientId, roomId, checkIn, checkOut, people, serviceIds);
            if (updated == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al actualizar reserva: " + e.getMessage());
        }
    }

    @Operation(summary = "Elimina una reserva por su ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        reservationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
