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
        String identifier = null;
        if (username != null && !username.trim().isEmpty()) {
            identifier = username.trim();
        } else if (email != null && !email.trim().isEmpty()) {
            identifier = email.trim();
        }

        if (identifier == null || password == null || password.trim().isEmpty()) {
            model.addAttribute("error", "Por favor ingresa tu correo/usuario y tu contraseña.");
            return "login";
        }

        Client client = clientService.login(identifier, password.trim());
        if (client == null) {
            model.addAttribute("error", "Usuario o contraseña incorrectos.");
            model.addAttribute("username", identifier);
            return "login";
        }

        if (client.isAdmin()) {
            return "redirect:/admin/rooms";
        }

        return "redirect:/profile/" + client.getId();
    }
}
