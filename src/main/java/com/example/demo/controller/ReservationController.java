package com.example.demo.controller;

import com.example.demo.entities.*;
import com.example.demo.service.ReservationManagerService;
import com.example.demo.service.RoomService;
import com.example.demo.service.ServiceService;
import com.example.demo.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/reservations")
public class ReservationController {

    @Autowired
    private ReservationManagerService reservationService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private ClientService clientService;

    @ModelAttribute("loggedClient")
    public Client getLoggedClient(@RequestParam(value = "clientId", required = false) Long clientId) {
        if (clientId != null) {
            try {
                return clientService.findById(clientId);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    // http://localhost:8080/reservations = mis reservas
    @GetMapping
    public String myReservations(@RequestParam Long clientId, Model model) {
        Client client;
        try {
            client = clientService.findById(clientId);
        } catch (Exception e) {
            return "redirect:/login";
        }
        try {
            List<Reservation> mine = reservationService.searchAll().stream()
                    .filter(r -> r.getClient() != null && r.getClient().getId().equals(client.getId()))
                    .toList();
            model.addAttribute("reservations", mine);
            return "my-reservations";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cargar tus reservas: " + e.getMessage());
            return "error";
        }
    }

    // http://localhost:8080/reservations/new
    @GetMapping("/new")
    public String newForm(@RequestParam Long clientId,
            @RequestParam(required = false) Long roomId,
            Model model) {
        Client client;
        try {
            client = clientService.findById(clientId);
            model.addAttribute("clientId", clientId);
        } catch (Exception e) {
            return "redirect:/login";
        }
        List<Room> availableRooms = roomService.findAll().stream()
                .filter(r -> r.getStatus() == RoomStatus.AVAILABLE)
                .toList();

        model.addAttribute("rooms", availableRooms);
        model.addAttribute("services", serviceService.searchAll().stream()
                .filter(s -> !s.isHidden())
                .toList());

        if (roomId != null) {
            boolean stillAvailable = availableRooms.stream().anyMatch(r -> r.getId().equals(roomId));
            if (stillAvailable) {
                model.addAttribute("preselectedRoomId", roomId);
            }
        }
        return "reservation-form";
    }

    @PostMapping("/new")
    public String create(@RequestParam Long clientId,
            @RequestParam Long roomId,
            @RequestParam String checkInDate,
            @RequestParam String checkOutDate,
            @RequestParam int numberOfPeople,
            @RequestParam(required = false) List<Long> serviceIds,
            Model model) {
        Client client;
        try {
            client = clientService.findById(clientId);
        } catch (Exception e) {
            return "redirect:/login";
        }
        try {
            Room room = roomService.findById(roomId);
            if (room.getStatus() != RoomStatus.AVAILABLE) {
                model.addAttribute("mensaje", "La habitación seleccionada ya no está disponible. Elige otra.");
                return "error";
            }

            LocalDate checkIn = LocalDate.parse(checkInDate);
            LocalDate checkOut = LocalDate.parse(checkOutDate);
            if (!checkOut.isAfter(checkIn)) {
                model.addAttribute("mensaje", "La fecha de salida debe ser posterior a la fecha de entrada.");
                return "error";
            }

            Reservation reservation = Reservation.builder()
                    .client(client)
                    .checkInDate(checkIn)
                    .checkOutDate(checkOut)
                    .numberOfPeople(numberOfPeople)
                    .status("CONFIRMED")
                    .build();

            ReservationRoom reservationRoom = ReservationRoom.builder()
                    .room(room)
                    .pricePerNight(room.getPricePerNight())
                    .build();
            reservation.addReservationRoom(reservationRoom); 

            if (serviceIds != null) {
                for (Long serviceId : serviceIds) {
                    com.example.demo.entities.Service service = serviceService.searchById(serviceId);
                    AcquiredService acquired = AcquiredService.builder()
                            .service(service)
                            .date(checkIn)
                            .quantity(1)
                            .unitPrice(service.getPrice() == null ? 0 : service.getPrice())
                            .build();

                    reservationRoom.addAcquiredService(acquired); 
                }
            }

            reservationService.save(reservation);
            return "redirect:/reservations?clientId=" + clientId + "&created=true";
        } catch (com.example.demo.errors.NotFoundException e) {
            throw e;
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al crear la reserva: " + e.getMessage());
            return "error";
        }
    }
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, @RequestParam Long clientId, Model model) {
        Client client;
        try {
            client = clientService.findById(clientId);
            model.addAttribute("clientId", clientId);
        } catch (Exception e) {
            return "redirect:/login";
        }
        Reservation reservation = reservationService.searchById(id);
        if (reservation.getClient() == null || !reservation.getClient().getId().equals(client.getId())) {
            model.addAttribute("mensaje", "No tienes permiso para editar esta reserva.");
            return "error";
        }

        ReservationRoom reservationRoom = reservation.getReservationRooms().get(0);
        java.util.Set<Long> selectedServiceIds = reservationRoom.getAcquiredServices().stream()
                .map(as -> as.getService().getId())
                .collect(java.util.stream.Collectors.toSet());

        model.addAttribute("reservation", reservation);
        model.addAttribute("services", serviceService.searchAll().stream()
                .filter(s -> !s.isHidden() || selectedServiceIds.contains(s.getId()))
                .toList());
        model.addAttribute("selectedServiceIds", selectedServiceIds); // nuevo
        return "reservation-edit";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id, @RequestParam Long clientId,
            @RequestParam String checkInDate,
            @RequestParam String checkOutDate,
            @RequestParam int numberOfPeople,
            @RequestParam(required = false) List<Long> serviceIds,
            Model model) {
        Client client;
        try {
            client = clientService.findById(clientId);
        } catch (Exception e) {
            return "redirect:/login";
        }
        try {
            Reservation reservation = reservationService.searchById(id);
            if (reservation.getClient() == null || !reservation.getClient().getId().equals(client.getId())) {
                model.addAttribute("mensaje", "No tienes permiso para editar esta reserva.");
                return "error";
            }

            LocalDate checkIn = LocalDate.parse(checkInDate);
            LocalDate checkOut = LocalDate.parse(checkOutDate);
            if (!checkOut.isAfter(checkIn)) {
                model.addAttribute("mensaje", "La fecha de salida debe ser posterior a la de entrada.");
                return "error";
            }

            reservation.setCheckInDate(checkIn);
            reservation.setCheckOutDate(checkOut);
            reservation.setNumberOfPeople(numberOfPeople);

            ReservationRoom reservationRoom = reservation.getReservationRooms().get(0);
            reservationRoom.getAcquiredServices().clear();
            if (serviceIds != null) {
                for (Long serviceId : serviceIds) {
                    com.example.demo.entities.Service service = serviceService.searchById(serviceId);
                    AcquiredService acquired = AcquiredService.builder()
                            .service(service)
                            .date(checkIn)
                            .quantity(1)
                            .unitPrice(service.getPrice() == null ? 0 : service.getPrice())
                            .build();

                    reservationRoom.addAcquiredService(acquired);
                }
            }

            reservationService.save(reservation);
            return "redirect:/reservations?clientId=" + clientId + "&updated=true";
        } catch (com.example.demo.errors.NotFoundException e) {
            throw e;
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al actualizar la reserva: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, @RequestParam Long clientId, Model model) {
        Client client;
        try {
            client = clientService.findById(clientId);
        } catch (Exception e) {
            return "redirect:/login";
        }
        try {
            Reservation reservation = reservationService.searchById(id);
            if (reservation.getClient() == null || !reservation.getClient().getId().equals(client.getId())) {
                model.addAttribute("mensaje", "No tienes permiso para eliminar esta reserva.");
                return "error";
            }
            reservationService.delete(id);
            return "redirect:/reservations?clientId=" + clientId + "&deleted=true";
        } catch (com.example.demo.errors.NotFoundException e) {
            throw e;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            model.addAttribute("mensaje", "No se pudo eliminar la reserva porque tiene registros asociados que lo impiden.");
            return "error";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al eliminar la reserva: " + e.getMessage());
            return "error";
        }
    }
}