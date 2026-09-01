package com.example.demo.controller;

import com.example.demo.entities.RoomType;
import com.example.demo.service.RoomTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/admin/room-types")
public class RoomTypeController {

    private final RoomTypeService service;

    @Autowired
    public RoomTypeController(RoomTypeService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        List<RoomType> list = service.findAll();
        model.addAttribute("roomTypes", list);
        return "room-types";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("roomType", new RoomType());
        return "room-type-form";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute RoomType roomType) {
        service.save(roomType);
        return "redirect:/admin/room-types";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        RoomType rt = service.findById(id);
        model.addAttribute("roomType", rt);
        return "room-type-form";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id, @ModelAttribute RoomType roomType) {
        roomType.setId(id);
        service.save(roomType);
        return "redirect:/admin/room-types";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/admin/room-types";
    }
}
