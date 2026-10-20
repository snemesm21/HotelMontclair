package com.example.demo.controller;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.dto.ReservationRequestDTO;
import com.example.demo.entities.Reservation;
import com.example.demo.service.ReservationManagerService;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "Obtiene un resumen de las reservas")
    @GetMapping("/summary")
    public List<ReservationDTO> getReservationSummaries() {
        return reservationService.getReservationSummaries();
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
    public ResponseEntity<?> createReservation(@RequestBody ReservationRequestDTO req) {
        try {
            Reservation saved = reservationService.createReservation(
                    req.getClientId(),
                    req.getRoomTypeId(),
                    req.getCheckInDate(),
                    req.getCheckOutDate(),
                    req.getNumberOfPeople(),
                    req.getServiceIds());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(saved);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));

        } catch (com.example.demo.errors.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al crear reserva: " + e.getMessage()));
        }
    }

    @Operation(summary = "Actualiza una reserva existente por su ID")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateReservation(
            @PathVariable Long id,
            @RequestBody ReservationRequestDTO req) {

        try {
            Reservation updated = reservationService.updateReservation(
                    id,
                    req.getClientId(),
                    req.getRoomTypeId(),
                    req.getCheckInDate(),
                    req.getCheckOutDate(),
                    req.getNumberOfPeople(),
                    req.getServiceIds());

            if (updated == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "No se encontró la reserva con ID " + id));
            }

            return ResponseEntity.ok(updated);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));

        } catch (com.example.demo.errors.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al actualizar reserva: " + e.getMessage()));
        }
    }

    @Operation(summary = "Elimina una reserva por su ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        reservationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
