package com.example.demo.controller;

import com.example.demo.entities.Room;
import com.example.demo.entities.RoomType;
import com.example.demo.service.RoomService;
import com.example.demo.service.RoomTypeService;
import com.example.demo.service.ServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class MainController {

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private RoomTypeService roomTypeService;

    @Autowired
    private RoomService roomService;

    public void setServiceService(ServiceService serviceService) {
        this.serviceService = serviceService;
    }

    public void setRoomTypeService(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    public void setRoomService(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping({ "/", "/index", "/home" })
    public String index(Model model) {
        try {
            model.addAttribute("services", serviceService.searchAll());
            model.addAttribute("roomTypes", roomTypeService.findAll());
            return "index";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cargar la página principal: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/rooms/cards")
    public String roomsCards(Model model) {
        try {
            model.addAttribute("roomTypes", roomTypeService.findAll());
            return "rooms-cards";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cargar el catálogo de habitaciones: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/rooms/type/{id}")
    public String roomType(@PathVariable Long id, Model model) {
        try {
            RoomType roomType = roomTypeService.findById(id);
            List<Room> rooms = roomService.findAll().stream()
                    .filter(room -> room.getType() != null && room.getType().getId().equals(id))
                    .collect(Collectors.toList());

            model.addAttribute("roomType", roomType);
            model.addAttribute("rooms", rooms);
            return "rooms-by-type";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cargar la categoría de habitación: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/rooms/{id}")
    public String roomDetail(@PathVariable Long id, Model model) {
        try {
            model.addAttribute("room", roomService.findById(id));
            return "room-detail";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cargar la habitación: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/gallery")
    public String gallery() {
        return "gallery";
    }

    @GetMapping("/location")
    public String location() {
        return "location";
    }
}