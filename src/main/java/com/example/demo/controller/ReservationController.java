package com.example.demo.controller;

import com.example.demo.entities.*;
import com.example.demo.service.ClientService;
import com.example.demo.service.ReservationManagerService;
import com.example.demo.service.RoomService;
import com.example.demo.service.ServiceService;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping({"/api/reservations"})
@CrossOrigin(origins = "http://localhost:4200")
public class ReservationController {

    @Autowired
    private ReservationManagerService reservationService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private ServiceService serviceService;

    public static class ReservationRequest {
        public Long id;
        public Long clientId;
        public Client client;
        public Long roomId;
        public Room room;
        public String checkInDate;
        public String checkOutDate;
        public Integer numberOfPeople;
        public String status;
        public List<Long> serviceIds;
        public List<ReservationRoom> reservationRooms;
    }

    private LocalDate parseLocalDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            if (dateStr.contains("T")) {
                dateStr = dateStr.substring(0, dateStr.indexOf("T"));
            }
            return LocalDate.parse(dateStr.trim());
        } catch (Exception e) {
            return null;
        }
    }

    @Operation(summary = "Obtiene todas las reservas")
    @GetMapping
    public List<Reservation> getAllReservations() {
        return reservationService.searchAll();
    }

    @Operation(summary = "Obtiene una reserva por su ID")
    @GetMapping("/{id}")
    public ResponseEntity<Reservation> getReservationById(@PathVariable Long id) {
        Reservation res = null;
        try {
            res = reservationService.searchById(id);
        } catch (Exception ignored) {}
        if (res == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(res);
    }

    @Operation(summary = "Obtiene las reservas de un cliente por su ID")
    @GetMapping("/client/{clientId}")
    public List<Reservation> getReservationsByClient(@PathVariable Long clientId) {
        return reservationService.searchAll().stream()
                .filter(r -> r.getClient() != null && r.getClient().getId().equals(clientId))
                .toList();
    }

    @Operation(summary = "Crea una nueva reserva")
    @PostMapping
    public ResponseEntity<?> createReservation(@RequestBody ReservationRequest req) {
        Long targetClientId = req.clientId != null ? req.clientId : (req.client != null ? req.client.getId() : null);
        Long targetRoomId = req.roomId != null ? req.roomId : (req.room != null ? req.room.getId() : null);
        if (targetRoomId == null && req.reservationRooms != null && !req.reservationRooms.isEmpty()) {
            ReservationRoom firstRr = req.reservationRooms.get(0);
            if (firstRr.getRoom() != null) {
                targetRoomId = firstRr.getRoom().getId();
            }
        }

        if (targetClientId == null || targetRoomId == null) {
            return ResponseEntity.badRequest().body("Cliente o habitación no encontrados.");
        }

        Client client = null;
        try {
            client = clientService.findById(targetClientId);
        } catch (Exception ignored) {}

        Room room = null;
        try {
            room = roomService.findById(targetRoomId);
        } catch (Exception ignored) {}

        if (client == null || room == null) {
            return ResponseEntity.badRequest().body("Cliente o habitación no encontrados.");
        }

        LocalDate checkIn = parseLocalDate(req.checkInDate);
        if (checkIn == null) checkIn = LocalDate.now();

        LocalDate checkOut = parseLocalDate(req.checkOutDate);
        if (checkOut == null) checkOut = checkIn.plusDays(1);

        Reservation reservation = Reservation.builder()
                .client(client)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .numberOfPeople(req.numberOfPeople != null ? req.numberOfPeople : 1)
                .status(req.status != null && !req.status.isBlank() ? req.status : "CONFIRMED")
                .build();

        ReservationRoom resRoom = ReservationRoom.builder()
                .room(room)
                .pricePerNight(room.getPricePerNight())
                .build();
        reservation.addReservationRoom(resRoom);

        List<Long> targetServiceIds = req.serviceIds;
        if (targetServiceIds == null && req.reservationRooms != null && !req.reservationRooms.isEmpty()) {
            ReservationRoom firstRr = req.reservationRooms.get(0);
            if (firstRr.getAcquiredServices() != null) {
                targetServiceIds = firstRr.getAcquiredServices().stream()
                        .filter(as -> as.getService() != null && as.getService().getId() != null)
                        .map(as -> as.getService().getId())
                        .toList();
            }
        }

        if (targetServiceIds != null) {
            for (Long sId : targetServiceIds) {
                try {
                    com.example.demo.entities.Service svc = serviceService.searchById(sId);
                    if (svc != null) {
                        AcquiredService acq = AcquiredService.builder()
                                .service(svc)
                                .date(checkIn)
                                .quantity(1)
                                .unitPrice(svc.getPrice() != null ? svc.getPrice() : 0.0)
                                .build();
                        resRoom.addAcquiredService(acq);
                    }
                } catch (Exception ignored) {}
            }
        }

        room.setStatus(RoomStatus.OCCUPIED);
        roomService.save(room);

        Reservation saved = reservationService.save(reservation);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Actualiza una reserva existente por su ID")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateReservation(@PathVariable Long id, @RequestBody ReservationRequest req) {
        Reservation reservation = null;
        try {
            reservation = reservationService.searchById(id);
        } catch (Exception e) {
            reservation = null;
        }

        if (reservation == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Reserva no encontrada con id: " + id);
        }

        Long targetClientId = req.clientId != null ? req.clientId : (req.client != null ? req.client.getId() : null);
        if (targetClientId != null) {
            try {
                Client client = clientService.findById(targetClientId);
                if (client != null) {
                    reservation.setClient(client);
                }
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Cliente no encontrado con id: " + targetClientId);
            }
        }

        LocalDate checkIn = parseLocalDate(req.checkInDate);
        if (checkIn != null) {
            reservation.setCheckInDate(checkIn);
        }
        LocalDate checkOut = parseLocalDate(req.checkOutDate);
        if (checkOut != null) {
            reservation.setCheckOutDate(checkOut);
        }
        if (req.numberOfPeople != null) {
            reservation.setNumberOfPeople(req.numberOfPeople);
        }
        if (req.status != null && !req.status.isBlank()) {
            reservation.setStatus(req.status);
        }

        Long targetRoomId = req.roomId != null ? req.roomId : (req.room != null ? req.room.getId() : null);
        if (targetRoomId == null && req.reservationRooms != null && !req.reservationRooms.isEmpty()) {
            ReservationRoom firstRr = req.reservationRooms.get(0);
            if (firstRr.getRoom() != null) {
                targetRoomId = firstRr.getRoom().getId();
            }
        }

        ReservationRoom resRoom;
        if (reservation.getReservationRooms() != null && !reservation.getReservationRooms().isEmpty()) {
            resRoom = reservation.getReservationRooms().get(0);
        } else {
            resRoom = ReservationRoom.builder().build();
            reservation.addReservationRoom(resRoom);
        }

        if (targetRoomId != null) {
            Room currentRoom = resRoom.getRoom();
            if (currentRoom == null || !currentRoom.getId().equals(targetRoomId)) {
                if (currentRoom != null) {
                    currentRoom.setStatus(RoomStatus.AVAILABLE);
                    roomService.save(currentRoom);
                }
                try {
                    Room newRoom = roomService.findById(targetRoomId);
                    if (newRoom != null) {
                        newRoom.setStatus(RoomStatus.OCCUPIED);
                        roomService.save(newRoom);
                        resRoom.setRoom(newRoom);
                        resRoom.setPricePerNight(newRoom.getPricePerNight());
                    }
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body("Habitación no encontrada con id: " + targetRoomId);
                }
            }
        }

        List<Long> targetServiceIds = req.serviceIds;
        if (targetServiceIds == null && req.reservationRooms != null && !req.reservationRooms.isEmpty()) {
            ReservationRoom firstRr = req.reservationRooms.get(0);
            if (firstRr.getAcquiredServices() != null) {
                targetServiceIds = firstRr.getAcquiredServices().stream()
                        .filter(as -> as.getService() != null && as.getService().getId() != null)
                        .map(as -> as.getService().getId())
                        .toList();
            }
        }

        if (targetServiceIds != null) {
            if (resRoom.getAcquiredServices() != null) {
                resRoom.getAcquiredServices().clear();
            } else {
                resRoom.setAcquiredServices(new ArrayList<>());
            }

            LocalDate svcDate = reservation.getCheckInDate() != null ? reservation.getCheckInDate() : LocalDate.now();
            for (Long sId : targetServiceIds) {
                try {
                    com.example.demo.entities.Service svc = serviceService.searchById(sId);
                    if (svc != null) {
                        AcquiredService acq = AcquiredService.builder()
                                .service(svc)
                                .date(svcDate)
                                .quantity(1)
                                .unitPrice(svc.getPrice() != null ? svc.getPrice() : 0.0)
                                .build();
                        resRoom.addAcquiredService(acq);
                    }
                } catch (Exception ignored) {}
            }
        }

        Reservation updated = reservationService.save(reservation);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Crea o actualiza una reserva")
    @PutMapping
    public ResponseEntity<?> createOrUpdateReservation(@RequestBody ReservationRequest req) {
        if (req.id != null) {
            return updateReservation(req.id, req);
        }
        return createReservation(req);
    }

    @Operation(summary = "Elimina una reserva por su ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        Reservation res = null;
        try {
            res = reservationService.searchById(id);
        } catch (Exception ignored) {}

        if (res != null && res.getReservationRooms() != null) {
            for (ReservationRoom rr : res.getReservationRooms()) {
                if (rr.getRoom() != null) {
                    Room r = rr.getRoom();
                    r.setStatus(RoomStatus.AVAILABLE);
                    roomService.save(r);
                }
            }
        }
        reservationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
