package com.example.demo.controller;

import com.example.demo.service.ReservationManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminReservationController {

    @Autowired
    private ReservationManagerService reservationService;

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
}
