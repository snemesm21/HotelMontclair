package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.errors.NotFoundException;
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
        List<Client> list = service.findAll();
        model.addAttribute("clients", list);
        return "clients";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("client", new Client());
        return "client-form";
    }

    @PostMapping("/add")
    public String add(@ModelAttribute Client client) {
        if (client.getRole() == null || client.getRole().isBlank()) {
            client.setRole("CLIENT");
        }
        Client saved = service.save(client);
        return "redirect:/profile/" + saved.getId();
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        try {
            Client c = service.findById(id);
            model.addAttribute("client", c);
            return "client-form";
        } catch (NotFoundException e) {
            model.addAttribute("mensaje", e.getMessage());
            return "error";
        }
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id, @ModelAttribute Client client, Model model) {
        try {
            Client existing = service.findById(id);
        client.setId(id);
        if (client.getPassword() == null || client.getPassword().isBlank()) {
            client.setPassword(existing.getPassword());
        }
        if (client.getAvatarUrl() == null || client.getAvatarUrl().isBlank()) {
            client.setAvatarUrl(existing.getAvatarUrl());
        }
        if (client.getRole() == null || client.getRole().isBlank()) {
            client.setRole(existing.getRole());
        }
        if (client.getUsername() == null || client.getUsername().isBlank()) {
            client.setUsername(existing.getUsername());
        }
        if (client.getEmail() == null || client.getEmail().isBlank()) {
            client.setEmail(existing.getEmail());
        }
        if (client.getFirstName() == null || client.getFirstName().isBlank()) {
            client.setFirstName(existing.getFirstName());
        }
        if (client.getLastName() == null || client.getLastName().isBlank()) {
            client.setLastName(existing.getLastName());
        }
        if (client.getPhone() == null || client.getPhone().isBlank()) {
            client.setPhone(existing.getPhone());
        }
        service.save(client);
        return "redirect:/clients";
        } catch (NotFoundException e) {
            model.addAttribute("mensaje", e.getMessage());
            return "error";
        }
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/clients";
    }
}
