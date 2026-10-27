package com.example.demo.controller;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.dto.ReservationRequestDTO;
import com.example.demo.entities.Reservation;
import com.example.demo.errors.NotFoundException;
import com.example.demo.service.ReservationManagerService;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST de reservas (/api/reservations).
 * Diseñado con arquitectura en capas.
 * Devuelve DTOs planos (ReservationDTO) sin objetos anidados ni grafos circulares hacia el frontend.
 * Maneja el ciclo de vida: activación de estadía, pago/limpieza de cuenta y finalización de estadía.
 */
@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "http://localhost:4200")
public class ReservationController {

    @Autowired
    private ReservationManagerService reservationService;

    /**
     * Lista todas las reservas en formato plano DTO (GET /api/reservations).
     * Evita devolver objetos anidados circulares hacia el front.
     */
    @Operation(summary = "Obtiene todas las reservas en formato DTO")
    @GetMapping
    public List<ReservationDTO> getAllReservations() {
        return reservationService.getReservationSummaries();
    }

    /**
     * Lista las reservas en formato resumido DTO (GET /api/reservations/summary).
     * Diseñado para las tablas de administración y listados de usuario.
     */
    @Operation(summary = "Obtiene un resumen de las reservas")
    @GetMapping("/summary")
    public List<ReservationDTO> getReservationSummaries() {
        return reservationService.getReservationSummaries();
    }

    /**
     * Busca una reserva por id (GET /api/reservations/{id}) devolviendo su DTO plano.
     */
    @Operation(summary = "Obtiene una reserva por su ID")
    @GetMapping("/{id}")
    public ResponseEntity<ReservationDTO> getReservationById(@PathVariable Long id) {
        ReservationDTO dto = reservationService.findSummaryById(id);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    /**
     * Lista las reservas de un cliente para su historial (GET /api/reservations/client/{clientId}).
     * Devuelve DTOs planos con datos completos de fecha, importes y estado.
     */
    @Operation(summary = "Obtiene el historial de reservas de un cliente por su ID")
    @GetMapping("/client/{clientId}")
    public List<ReservationDTO> getReservationsByClientId(@PathVariable Long clientId) {
        return reservationService.getSummariesByClientId(clientId);
    }

    /**
     * Crea una nueva reserva (POST /api/reservations).
     * Recibe ReservationRequestDTO y devuelve el DTO plano resultante.
     */
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

            ReservationDTO dto = reservationService.findSummaryById(saved.getId());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(dto != null ? dto : saved);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));

        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al crear reserva: " + e.getMessage()));
        }
    }

    /**
     * Actualiza una reserva existente (PUT /api/reservations/{id}).
     */
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

            ReservationDTO dto = reservationService.findSummaryById(updated.getId());

            return ResponseEntity.ok(dto != null ? dto : updated);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));

        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al actualizar reserva: " + e.getMessage()));
        }
    }

    /**
     * Activa la estadía de un cliente cuando este llega al hotel (PATCH /api/reservations/{id}/activate).
     * El estado pasa a ACTIVE.
     */
    @Operation(summary = "Activa la estadía de un cliente (cambia estado a ACTIVO)")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<?> activateStay(@PathVariable Long id) {
        try {
            ReservationDTO updated = reservationService.activateStay(id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al activar estadía: " + e.getMessage()));
        }
    }

    /**
     * Paga la cuenta de una estadía (PATCH /api/reservations/{id}/pay).
     * Limpia la cuenta del cliente (paid = true, saldo pendiente = 0.0).
     */
    @Operation(summary = "Paga la cuenta de una estadía (se limpia la cuenta del cliente)")
    @PatchMapping("/{id}/pay")
    public ResponseEntity<?> payStay(@PathVariable Long id) {
        try {
            ReservationDTO updated = reservationService.payStayAccount(id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al pagar cuenta: " + e.getMessage()));
        }
    }

    /**
     * Finaliza la estadía de un cliente (PATCH /api/reservations/{id}/finish).
     * Estado pasa a INACTIVE.
     * REGLA DE NEGOCIO: Solo se permite realizar este procedimiento si la cuenta está vacía (ya pagó).
     */
    @Operation(summary = "Acaba la estadía de un cliente (cambia estado a INACTIVO; requiere cuenta pagada)")
    @PatchMapping("/{id}/finish")
    public ResponseEntity<?> finishStay(@PathVariable Long id) {
        try {
            ReservationDTO updated = reservationService.finishStay(id);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            // Rechazo de negocio cuando la cuenta no está saldada
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al finalizar estadía: " + e.getMessage()));
        }
    }

    /**
     * Cancela una reserva (PATCH /api/reservations/{id}/cancel).
     * Pone la reserva como cancelada para no eliminarla de la base de datos y mantener el historial.
     * REGLA DE NEGOCIO 4: Solo se pueden cancelar reservas que todavia no estan activas,
     * es decir que su fecha de inicio es posterior a la fecha actual.
     */
    @Operation(summary = "Cancela una reserva (la pasa a estado CANCELADA en lugar de eliminarla)")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<?> cancelReservation(@PathVariable Long id) {
        try {
            ReservationDTO updated = reservationService.cancelReservation(id);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al cancelar reserva: " + e.getMessage()));
        }
    }

    /**
     * Asigna un servicio consumido a una habitacion (POST /api/reservations/rooms/{roomId}/services).
     * REGLA DE NEGOCIO 5: En caso de que en la habitacion no haya ninguna reserva activa,
     * no se debe poder asignar ningun servicio.
     */
    @Operation(summary = "Asigna un servicio consumido a una habitacion (requiere reserva activa)")
    @PostMapping("/rooms/{roomId}/services")
    public ResponseEntity<?> assignServiceToRoom(
            @PathVariable Long roomId,
            @RequestBody Map<String, Object> body) {
        try {
            if (!body.containsKey("serviceId")) {
                return ResponseEntity.badRequest().body(Map.of("message", "El serviceId es obligatorio."));
            }
            Long serviceId = Long.valueOf(body.get("serviceId").toString());
            int quantity = body.containsKey("quantity") ? Integer.parseInt(body.get("quantity").toString()) : 1;

            ReservationDTO updated = reservationService.assignServiceToRoom(roomId, serviceId, quantity);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al asignar servicio: " + e.getMessage()));
        }
    }

    /**
     * Elimina un servicio contratado de la cuenta de una reserva (DELETE /api/reservations/{id}/services/{acquiredServiceId}).
     */
    @Operation(summary = "Elimina un servicio contratado de la cuenta de una reserva")
    @DeleteMapping("/{id}/services/{acquiredServiceId}")
    public ResponseEntity<?> removeServiceFromReservation(
            @PathVariable Long id,
            @PathVariable Long acquiredServiceId) {
        try {
            ReservationDTO updated = reservationService.removeServiceFromReservation(id, acquiredServiceId);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al eliminar servicio de la cuenta: " + e.getMessage()));
        }
    }

    /**
     * Elimina una reserva (DELETE /api/reservations/{id}).
     * Implementa la logica de historial: no se borra fisicamente, sino que se cancela logicamente.
     * REGLA DE NEGOCIO 4: Solo se pueden cancelar reservas que todavia no estan activas,
     * es decir que su fecha de inicio es posterior a la fecha actual.
     */
    @Operation(summary = "Cancela logicamente una reserva por su ID para conservar el historial")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReservation(@PathVariable Long id) {
        try {
            reservationService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
