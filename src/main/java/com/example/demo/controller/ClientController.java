package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/clients")
public class ClientController {

    @Autowired
    private ClientService service;

    @GetMapping
    public String list(Model model) {
        try {
            List<Client> list = service.findAll();
            model.addAttribute("clients", list);
            return "clients";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cargar los clientes: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/add")
    public String addForm(@RequestParam(required = false, defaultValue = "false") boolean admin, Model model) {
        model.addAttribute("client", new Client());
        model.addAttribute("isAdmin", admin);
        return "client-form";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute Client client, Model model) {
        try {
            Client saved = service.save(client);
            return "redirect:/profile/" + saved.getId();
        } catch (IllegalArgumentException e) {
            model.addAttribute("mensaje", e.getMessage());
            return "error";
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            model.addAttribute("mensaje", "No se pudo registrar: El nombre de usuario o el correo electrónico ya están en uso.");
            return "error";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al registrar cliente: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        try {
            Client c = service.findById(id);
            model.addAttribute("client", c);
            model.addAttribute("isAdmin", true); // Edit is only for admin right now
            return "client-form";
        } catch (com.example.demo.errors.NotFoundException e) {
            throw e;
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al cargar formulario de edición: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id, @ModelAttribute Client client, Model model) {
        try {
            client.setId(id);
            service.save(client);
            return "redirect:/clients";
        } catch (IllegalArgumentException e) {
            model.addAttribute("mensaje", e.getMessage());
            return "error";
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            model.addAttribute("mensaje", "No se pudo actualizar: El nombre de usuario o el correo electrónico ya están en uso.");
            return "error";
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al actualizar cliente: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, Model model) {
        try {
            service.delete(id);
            return "redirect:/clients";
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            model.addAttribute("mensaje", "No se puede eliminar el cliente porque tiene registros asociados (por ejemplo, reservaciones).");
            return "error";
        } catch (com.example.demo.errors.NotFoundException e) {
            throw e;
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error al eliminar cliente: " + e.getMessage());
            return "error";
        }
    }
}
