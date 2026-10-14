package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.service.ClientService;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping({"/api/auth"})
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private ClientService clientService;

    public static class LoginRequest {
        public String identifier;
        public String password;
    }

    @Operation (summary = "Inicia sesión con correo electrónico o número de teléfono y contraseña")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (req == null || req.identifier == null || req.password == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Credenciales incompletas."));
        }
        Client client = clientService.login(req.identifier, req.identifier, req.password);
        if (client != null) {
            String redirectUrl = client.isAdmin() ? "/admin/rooms" : "/profile/" + client.getId();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "client", client,
                "role", client.getRole(),
                "redirectUrl", redirectUrl
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Usuario o contraseña incorrectos."));
        }
    }
}
