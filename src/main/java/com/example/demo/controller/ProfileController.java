package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final ClientService clientService;

    @Autowired
    public ProfileController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("/{id}")
    public String viewProfile(@PathVariable Long id, Model model) {
        Client client = clientService.findById(id);
        if (client == null) {
            return "redirect:/login";
        }
        model.addAttribute("client", client);
        return "profile";
    }

    @PostMapping("/edit/{id}")
    public String editProfile(@PathVariable Long id, @ModelAttribute Client client) {
        Client existing = clientService.findById(id);
        if (existing == null) {
            return "redirect:/login";
        }
        client.setId(id);
        if (client.getPassword() == null || client.getPassword().isBlank()) {
            client.setPassword(existing.getPassword());
        }
        if (client.getAvatarUrl() == null || client.getAvatarUrl().isBlank()) {
            client.setAvatarUrl(existing.getAvatarUrl());
        }
        clientService.save(client);
        return "redirect:/profile/" + id + "?updated=true";
    }

    @PostMapping("/delete/{id}")
    public String deleteProfilePost(@PathVariable Long id) {
        clientService.delete(id);
        return "redirect:/login?deleted=true";
    }

    @GetMapping("/delete/{id}")
    public String deleteProfileGet(@PathVariable Long id) {
        clientService.delete(id);
        return "redirect:/login?deleted=true";
    }
}
