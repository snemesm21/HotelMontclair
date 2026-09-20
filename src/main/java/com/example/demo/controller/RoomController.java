package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.demo.entities.Room;
import com.example.demo.entities.RoomType;
import com.example.demo.service.RoomService;
import com.example.demo.service.RoomTypeService;

@Controller
public class RoomController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private RoomTypeService roomTypeService;

    @GetMapping({ "/rooms", "/rooms/cards" })
    public String showCards(Model model) {
        try {
            model.addAttribute("roomTypes", roomTypeService.findAll());
            return "rooms-cards";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error inesperado al cargar las habitaciones: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/rooms/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        try {
            Room room = roomService.findById(id);
            model.addAttribute("room", room);
            return "room-detail";
        } catch (com.example.demo.errors.NotFoundException e) {
            throw e;
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error inesperado al cargar la habitación: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/rooms/type/{id}")
    public String roomsByType(@PathVariable("id") Long id, Model model) {
        try {
            RoomType type = roomTypeService.findById(id);
            List<Room> rooms = roomService.findAll().stream()
                .filter(r -> r.getType() != null && r.getType().getId().equals(id))
                .toList();
            model.addAttribute("roomType", type);
            model.addAttribute("rooms", rooms);
            return "rooms-by-type";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cargar las habitaciones del tipo especificado: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/admin/rooms")
    public String listRooms(Model model) {
        try {
            List<Room> rooms = roomService.findAll();
            model.addAttribute("rooms", rooms);
            model.addAttribute("roomTypes", roomTypeService.findAll());
            return "rooms";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al listar las habitaciones: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/admin/rooms/add")
    public String showAddForm(Model model) {
        model.addAttribute("room", new Room());
        model.addAttribute("roomTypes", roomTypeService.findAll());
        return "room-form";
    }

    @GetMapping("/admin/rooms/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        try {
            Room room = roomService.findById(id);
            if (room.getType() != null) {
                room.setTypeId(room.getType().getId());
            }
            model.addAttribute("room", room);
            model.addAttribute("roomTypes", roomTypeService.findAll());
            return "room-form";
        } catch (com.example.demo.errors.NotFoundException e) {
            throw e;
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cargar la habitación: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/admin/rooms/save")
    public String saveRoom(Room room, Model model) {
        try {
            roomService.save(room);
            return "redirect:/admin/rooms";
        } catch (IllegalArgumentException e) {
            model.addAttribute("mensaje", e.getMessage());
            return "error";
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            model.addAttribute("mensaje", "No se pudo guardar: Ya existe una habitación con el número '" + room.getNumber() + "'.");
            return "error";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al guardar la habitación: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/admin/rooms/delete/{id}")
    public String deleteRoom(@PathVariable("id") Long id, Model model) {
        try {
            roomService.delete(id);
            return "redirect:/admin/rooms";
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            model.addAttribute("mensaje", "No se puede eliminar la habitación porque tiene registros asociados (por ejemplo, reservaciones).");
            return "error";
        } catch (com.example.demo.errors.NotFoundException e) {
            throw e;
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al eliminar la habitación: " + e.getMessage());
            return "error";
        }
    }
}
