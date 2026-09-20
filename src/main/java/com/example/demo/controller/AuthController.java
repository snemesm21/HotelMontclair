package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @Autowired
    private ClientService clientService;

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "password", required = false) String password,
            Model model) {
        try {
            Client client;
            try {
                client = clientService.login(username, email, password);
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", e.getMessage());
                return "login";
            }

            if (client == null) {
                model.addAttribute("error", "Usuario o contraseña incorrectos.");
                String identifier = (username != null && !username.trim().isEmpty()) ? username.trim() : (email != null ? email.trim() : "");
                model.addAttribute("username", identifier);
                return "login";
            }

            if (client.isAdmin()) {
                return "redirect:/admin/rooms";
            }

            return "redirect:/profile/" + client.getId();
        } catch (Exception e) {
            model.addAttribute("mensaje", "Error inesperado durante el login: " + e.getMessage());
            return "error";
        }
    }
}
