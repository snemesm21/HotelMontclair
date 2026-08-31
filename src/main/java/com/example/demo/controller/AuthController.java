package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.service.ClientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final ClientService clientService;

    public AuthController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("/login")
    public String loginForm(Model model) {
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam(value = "username", required = false) String username,
                               @RequestParam(value = "email", required = false) String email,
                               @RequestParam(value = "password", required = false) String password,
                               Model model) {
        String identifier = (username != null && !username.isBlank()) ? username : email;

        if (identifier == null || identifier.isBlank() || password == null || password.isBlank()) {
            model.addAttribute("error", "Por favor ingresa tu usuario o correo y tu contraseña.");
            return "login";
        }

        Client client = clientService.login(identifier, password);
        if (client == null) {
            model.addAttribute("error", "Credenciales inválidas. El usuario/correo o la contraseña son incorrectos.");
            model.addAttribute("username", identifier);
            return "login";
        }

        model.addAttribute("client", client);
        model.addAttribute("success", "¡Credenciales válidas! Inicio de sesión exitoso. Bienvenido(a), " + client.getFirstName() + " " + client.getLastName() + ".");
        return "login";
    }
}
