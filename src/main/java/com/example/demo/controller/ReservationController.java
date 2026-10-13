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
        public Long clientId;
        public Long roomId;
        public String checkInDate;
        public String checkOutDate;
        public Integer numberOfPeople;
        public List<Long> serviceIds;
    }

    @Operation (summary = "Obtiene todas las reservas")
    @GetMapping
    public List<Reservation> getAllReservations() {
        return reservationService.searchAll();
    }

    @Operation (summary = "Obtiene una reserva por su ID")
    @GetMapping("/{id}")
    public ResponseEntity<Reservation> getReservationById(@PathVariable Long id) {
        Reservation res = reservationService.searchById(id);
        if (res == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(res);
    }

    @Operation (summary = "Obtiene todas las reservas de un cliente por su ID")
    @GetMapping("/client/{clientId}")
    public List<Reservation> getReservationsByClient(@PathVariable Long clientId) {
        return reservationService.searchAll().stream()
                .filter(r -> r.getClient() != null && r.getClient().getId().equals(clientId))
                .toList();
    }

    @Operation (summary = "Crea una nueva reserva")
    @PostMapping
    public ResponseEntity<?> createReservation(@RequestBody ReservationRequest req) {
        Client client = clientService.findById(req.clientId);
        Room room = roomService.findById(req.roomId);
        if (client == null || room == null) {
            return ResponseEntity.badRequest().body("Cliente o habitaciA3n no encontrados.");
        }

        LocalDate checkIn = LocalDate.parse(req.checkInDate);
        LocalDate checkOut = LocalDate.parse(req.checkOutDate);

        Reservation reservation = Reservation.builder()
                .client(client)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .numberOfPeople(req.numberOfPeople != null ? req.numberOfPeople : 1)
                .status("CONFIRMED")
                .build();

        ReservationRoom resRoom = ReservationRoom.builder()
                .room(room)
                .pricePerNight(room.getPricePerNight())
                .build();
        reservation.addReservationRoom(resRoom);

        if (req.serviceIds != null) {
            for (Long sId : req.serviceIds) {
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
            }
        }

        room.setStatus(RoomStatus.OCCUPIED);
        roomService.save(room);

        Reservation saved = reservationService.save(reservation);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation (summary = "Elimina una reserva por su ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        Reservation res = reservationService.searchById(id);
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
