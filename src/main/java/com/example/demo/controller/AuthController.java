package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.service.ClientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {
    private final ClientService clientService;

    public AuthController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("/login")
    public String loginForm() {
        return "login"; // thymeleaf template login.html
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String username,
                               @RequestParam String password,
                               Model model) {
        Client client = clientService.login(username, password);
        if (client == null) {
            model.addAttribute("error", "Credenciales inválidas");
            return "login";
        }
        // guardamos el cliente en la sesión (simplemente en modelo por ahora)
        model.addAttribute("client", client);
        return "redirect:/home"; // puede redirigir a la página principal
    }
}

