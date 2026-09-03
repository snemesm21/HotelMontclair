package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.demo.entities.Room;
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
        List<Room> rooms = roomService.findAll();
        model.addAttribute("rooms", rooms);
        return "rooms-cards";
    }

    @GetMapping("/rooms/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        Room room = roomService.findById(id);
        if (room == null) {
            return "redirect:/rooms/cards";
        }
        model.addAttribute("room", room);
        return "room-detail";
    }

    @GetMapping("/admin/rooms")
    public String listRooms(Model model) {
        List<Room> rooms = roomService.findAll();
        model.addAttribute("rooms", rooms);
        model.addAttribute("roomTypes", roomTypeService.findAll());
        return "rooms";
    }

    @GetMapping("/admin/rooms/add")
    public String showAddForm(Model model) {
        model.addAttribute("room", new Room());
        model.addAttribute("roomTypes", roomTypeService.findAll());
        return "room-form";
    }

    @GetMapping("/admin/rooms/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Room room = roomService.findById(id);
        model.addAttribute("room", room);
        model.addAttribute("roomTypes", roomTypeService.findAll());
        return "room-form";
    }

    @PostMapping("/admin/rooms/save")
    public String saveRoom(Room room) {
        roomService.save(room);
        return "redirect:/admin/rooms";
    }

    @GetMapping("/admin/rooms/delete/{id}")
    public String deleteRoom(@PathVariable("id") Long id) {
        roomService.delete(id);
        return "redirect:/admin/rooms";
    }
}
