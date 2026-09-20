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

    @Autowired
    private RoomTypeService service;

    @GetMapping
    public String list(Model model) {
        try {
            List<RoomType> list = service.findAll();
            model.addAttribute("roomTypes", list);
            return "room-types";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al listar tipos de habitación: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("roomType", new RoomType());
        return "room-type-form";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute RoomType roomType, Model model) {
        try {
            service.save(roomType);
            return "redirect:/admin/room-types";
        } catch (IllegalArgumentException e) {
            model.addAttribute("mensaje", e.getMessage());
            return "error";
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            model.addAttribute("mensaje", "No se pudo guardar: Ya existe un tipo de habitación con el nombre '" + roomType.getName() + "'.");
            return "error";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al guardar tipo de habitación: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        try {
            RoomType rt = service.findById(id);
            model.addAttribute("roomType", rt);
            return "room-type-form";
        } catch (com.example.demo.errors.NotFoundException e) {
            throw e;
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cargar tipo de habitación: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id, @ModelAttribute RoomType roomType, Model model) {
        try {
            roomType.setId(id);
            service.save(roomType);
            return "redirect:/admin/room-types";
        } catch (IllegalArgumentException e) {
            model.addAttribute("mensaje", e.getMessage());
            return "error";
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            model.addAttribute("mensaje", "No se pudo actualizar: Ya existe un tipo de habitación con el nombre '" + roomType.getName() + "'.");
            return "error";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al actualizar tipo de habitación: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, Model model) {
        try {
            service.delete(id);
            return "redirect:/admin/room-types";
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            model.addAttribute("mensaje", "No se puede eliminar el tipo de habitación porque hay habitaciones asociadas a él. Por favor, elimine o reasigne las habitaciones primero.");
            return "error";
        } catch (com.example.demo.errors.NotFoundException e) {
            throw e;
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al eliminar tipo de habitación: " + e.getMessage());
            return "error";
        }
    }
}
