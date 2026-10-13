package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping({"/api/auth", "/auth"})
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private ClientService clientService;

    public static class LoginRequest {
        public String identifier;
        public String password;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (req == null || req.identifier == null || req.password == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Credenciales incompletas."));
        }
        Client client = clientService.login(req.identifier, req.identifier, req.password);
        if (client != null) {
            return ResponseEntity.ok(Map.of("success", true, "client", client));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Usuario o contraseAa incorrectos."));
        }
    }
}
