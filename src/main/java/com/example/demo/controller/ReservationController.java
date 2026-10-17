
package com.example.demo.controller;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.entities.AcquiredService;
import com.example.demo.entities.Reservation;
import com.example.demo.entities.ReservationRoom;
import com.example.demo.service.ReservationManagerService;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
                return reservationService.searchAll()
                                .stream()
                                .map(this::toDTO)
                                .toList();
        }

        private ReservationDTO toDTO(Reservation reservation) {

                ReservationRoom reservationRoom = null;

                if (reservation.getReservationRooms() != null
                                && !reservation.getReservationRooms().isEmpty()) {
                        reservationRoom = reservation.getReservationRooms().get(0);
                }

                String roomNumber = "";
                String roomName = "";
                String roomTypeName = "";

                double pricePerNight = 0;
                double servicesSubtotal = 0;

                List<ReservationDTO.ServiceDetail> services = new ArrayList<>();

                if (reservationRoom != null) {

                        pricePerNight = reservationRoom.getPricePerNight();

                        if (reservationRoom.getRoom() != null) {

                                roomNumber = reservationRoom.getRoom().getNumber();

                                roomName = reservationRoom.getRoom().getName();

                                if (reservationRoom.getRoom().getRoomType() != null) {
                                        roomTypeName = reservationRoom.getRoom()
                                                        .getRoomType().getName();
                                }
                        }

                        if (reservationRoom.getAcquiredServices() != null) {

                                for (AcquiredService acquired : reservationRoom.getAcquiredServices()) {

                                        String serviceName = "";

                                        if (acquired.getService() != null) {
                                                serviceName = acquired.getService().getName();
                                        }

                                        double subtotal = acquired.getQuantity()
                                                        * acquired.getUnitPrice();

                                        servicesSubtotal += subtotal;

                                        services.add(
                                                        ReservationDTO.ServiceDetail.builder()
                                                                        .id(acquired.getId())
                                                                        .name(serviceName)
                                                                        .quantity(acquired.getQuantity())
                                                                        .unitPrice(acquired.getUnitPrice())
                                                                        .subtotal(subtotal)
                                                                        .build());
                                }
                        }
                }

                long nights = ChronoUnit.DAYS.between(
                                reservation.getCheckInDate(),
                                reservation.getCheckOutDate());

                if (nights < 1) {
                        nights = 1;
                }

                double roomSubtotal = pricePerNight * nights;

                double totalPrice = roomSubtotal + servicesSubtotal;

                return ReservationDTO.builder()
                                .id(reservation.getId())
                                .clientFirstName(
                                                reservation.getClient().getFirstName())
                                .clientLastName(
                                                reservation.getClient().getLastName())
                                .clientEmail(
                                                reservation.getClient().getEmail())
                                .clientPhone(
                                                reservation.getClient().getPhone())
                                .roomNumber(roomNumber)
                                .roomName(roomName)
                                .roomTypeName(roomTypeName)
                                .pricePerNight(pricePerNight)
                                .checkInDate(reservation.getCheckInDate())
                                .checkOutDate(reservation.getCheckOutDate())
                                .numberOfPeople(reservation.getNumberOfPeople())
                                .status(reservation.getStatus())
                                .nights(nights)
                                .roomSubtotal(roomSubtotal)
                                .servicesSubtotal(servicesSubtotal)
                                .totalPrice(totalPrice)
                                .services(services)
                                .build();
        }

        @Operation(summary = "Obtiene una reserva por su ID")
        @GetMapping("/{id}")
        public ResponseEntity<Reservation> getReservationById(
                        @PathVariable Long id) {

                Reservation res = reservationService.searchById(id);

                if (res == null) {
                        return ResponseEntity.notFound().build();
                }

                return ResponseEntity.ok(res);
        }

        @Operation(summary = "Obtiene las reservas de un cliente por su ID")
        @GetMapping("/client/{clientId}")
        public List<Reservation> getReservationsByClientId(
                        @PathVariable Long clientId) {

                return reservationService.searchByClientId(clientId);
        }

        @Operation(summary = "Crea una nueva reserva")
        @PostMapping
        public ResponseEntity<?> createReservation(
                        @RequestBody Map<String, Object> req) {

                try {
                        Long clientId = req.get("clientId") != null
                                        ? Long.valueOf(req.get("clientId").toString())
                                        : null;

                        Long roomTypeId = req.get("roomTypeId") != null
                                        ? Long.valueOf(req.get("roomTypeId").toString())
                                        : null;

                        LocalDate checkIn = req.get("checkInDate") != null
                                        ? LocalDate.parse(
                                                        req.get("checkInDate").toString())
                                        : null;

                        LocalDate checkOut = req.get("checkOutDate") != null
                                        ? LocalDate.parse(
                                                        req.get("checkOutDate").toString())
                                        : null;

                        int people = req.get("numberOfPeople") != null
                                        ? Integer.parseInt(
                                                        req.get("numberOfPeople").toString())
                                        : 1;

                        List<Long> serviceIds = null;

                        if (req.get("serviceIds") instanceof List<?> rawList) {
                                serviceIds = rawList.stream()
                                                .map(s -> Long.valueOf(s.toString()))
                                                .toList();
                        }

                        Reservation saved = reservationService.createReservation(
                                        clientId,
                                        roomTypeId,
                                        checkIn,
                                        checkOut,
                                        people,
                                        serviceIds);

                        return ResponseEntity
                                        .status(HttpStatus.CREATED)
                                        .body(saved);

                } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest()
                                        .body(e.getMessage());

                } catch (Exception e) {
                        return ResponseEntity
                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body("Error al crear reserva: " + e.getMessage());
                }
        }

        @Operation(summary = "Actualiza una reserva existente por su ID")
        @PutMapping("/{id}")
        public ResponseEntity<?> updateReservation(
                        @PathVariable Long id,
                        @RequestBody Map<String, Object> req) {

                try {
                        Long clientId = req.get("clientId") != null
                                        ? Long.valueOf(req.get("clientId").toString())
                                        : null;

                        Long roomTypeId = req.get("roomTypeId") != null
                                        ? Long.valueOf(req.get("roomTypeId").toString())
                                        : null;

                        LocalDate checkIn = req.get("checkInDate") != null
                                        ? LocalDate.parse(
                                                        req.get("checkInDate").toString())
                                        : null;

                        LocalDate checkOut = req.get("checkOutDate") != null
                                        ? LocalDate.parse(
                                                        req.get("checkOutDate").toString())
                                        : null;

                        int people = req.get("numberOfPeople") != null
                                        ? Integer.parseInt(
                                                        req.get("numberOfPeople").toString())
                                        : 1;

                        List<Long> serviceIds = null;

                        if (req.get("serviceIds") instanceof List<?> rawList) {
                                serviceIds = rawList.stream()
                                                .map(s -> Long.valueOf(s.toString()))
                                                .toList();
                        }

                        Reservation updated = reservationService.updateReservation(
                                        id,
                                        clientId,
                                        roomTypeId,
                                        checkIn,
                                        checkOut,
                                        people,
                                        serviceIds);

                        if (updated == null) {
                                return ResponseEntity.notFound().build();
                        }

                        return ResponseEntity.ok(updated);

                } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest()
                                        .body(e.getMessage());

                } catch (Exception e) {
                        return ResponseEntity
                                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body("Error al actualizar reserva: " + e.getMessage());
                }
        }

        @Operation(summary = "Elimina una reserva por su ID")
        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteReservation(
                        @PathVariable Long id) {

                reservationService.delete(id);
                return ResponseEntity.noContent().build();
        }
}
