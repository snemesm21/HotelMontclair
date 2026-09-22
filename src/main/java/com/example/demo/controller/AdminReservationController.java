package com.example.demo.controller;

import com.example.demo.entities.*;
import com.example.demo.service.ClientService;
import com.example.demo.service.ReservationManagerService;
import com.example.demo.service.RoomService;
import com.example.demo.service.ServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminReservationController {

    @Autowired
    private ReservationManagerService reservationService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private ServiceService serviceService;

    @GetMapping("/reservations")
    public String viewReservations(Model model) {
        try {
            model.addAttribute("reservations", reservationService.searchAll());
            return "reservations-admin";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cargar las reservas: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/reservations/new")
    public String newForm(Model model) {
        model.addAttribute("clients", clientService.findAll());
        model.addAttribute("rooms", roomService.findAll().stream()
                .filter(r -> r.getStatus() == RoomStatus.AVAILABLE).toList());
        model.addAttribute("services", serviceService.searchAll().stream()
                .filter(s -> !s.isHidden())
                .toList());
        return "admin-reservation-form";
    }

    @PostMapping("/reservations/new")
    public String create(@RequestParam Long clientId,
            @RequestParam Long roomId,
            @RequestParam String checkInDate,
            @RequestParam String checkOutDate,
            @RequestParam int numberOfPeople,
            @RequestParam(required = false) List<Long> serviceIds,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            Client client = clientService.findById(clientId);
            Room room = roomService.findById(roomId);
            if (room.getStatus() != RoomStatus.AVAILABLE) {
                model.addAttribute("mensaje", "La habitación seleccionada ya no está disponible.");
                return "error";
            }

            LocalDate checkIn = LocalDate.parse(checkInDate);
            LocalDate checkOut = LocalDate.parse(checkOutDate);
            if (!checkOut.isAfter(checkIn)) {
                model.addAttribute("mensaje", "La fecha de salida debe ser posterior a la de entrada.");
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
            redirectAttributes.addFlashAttribute("successMessage", "Reserva creada con éxito.");
            return "redirect:/admin/reservations";
        } catch (com.example.demo.errors.NotFoundException e) {
            throw e;
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al crear la reserva: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/reservations/delete/{id}")
    public String deleteReservation(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            reservationService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Reserva eliminada con éxito.");
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error de integridad: No se puede eliminar la reserva porque tiene registros asociados que lo impiden.");
        } catch (com.example.demo.errors.NotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error al eliminar la reserva: " + e.getMessage());
        }
        return "redirect:/admin/reservations";
    }

    @GetMapping("/reservations/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Reservation reservation = reservationService.searchById(id);

        ReservationRoom reservationRoom = reservation.getReservationRooms().get(0);
        java.util.Set<Long> selectedServiceIds = reservationRoom.getAcquiredServices().stream()
                .map(as -> as.getService().getId())
                .collect(java.util.stream.Collectors.toSet());

        model.addAttribute("reservation", reservation);
        model.addAttribute("clients", clientService.findAll());
        model.addAttribute("services", serviceService.searchAll().stream()
                .filter(s -> !s.isHidden() || selectedServiceIds.contains(s.getId()))
                .toList());
        model.addAttribute("selectedServiceIds", selectedServiceIds); // nuevo
        return "admin-reservation-edit";
    }

    @PostMapping("/reservations/edit/{id}")
    public String edit(@PathVariable Long id,
            @RequestParam Long clientId,
            @RequestParam String checkInDate,
            @RequestParam String checkOutDate,
            @RequestParam int numberOfPeople,
            @RequestParam(required = false) List<Long> serviceIds,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            Reservation reservation = reservationService.searchById(id);
            Client client = clientService.findById(clientId);

            LocalDate checkIn = LocalDate.parse(checkInDate);
            LocalDate checkOut = LocalDate.parse(checkOutDate);
            if (!checkOut.isAfter(checkIn)) {
                model.addAttribute("mensaje", "La fecha de salida debe ser posterior a la de entrada.");
                return "error";
            }

            reservation.setClient(client);
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
            redirectAttributes.addFlashAttribute("successMessage", "Reserva actualizada con éxito.");
            return "redirect:/admin/reservations";
        } catch (com.example.demo.errors.NotFoundException e) {
            throw e;
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al actualizar la reserva: " + e.getMessage());
            return "error";
        }
    }
}