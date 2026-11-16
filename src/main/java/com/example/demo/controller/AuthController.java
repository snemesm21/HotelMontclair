package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.security.JWTGenerator;
import com.example.demo.service.ClientService;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/*Controlador REST de autenticación (/api/auth).
No usa tokens: si las credenciales son correctas devuelve el cliente completo
y el frontend lo guarda como sesión.*/
@RestController
@RequestMapping({"/api/auth"})
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private ClientService clientService;

    @Autowired 
    private AuthenticationManager authenticationManager;

    @Autowired 
    private JWTGenerator jwtGenerator;

    //Cuerpo esperado en la petición de login.
    public static class LoginRequest {
        public String identifier;
        public String password;
    }

    /*Inicia sesión (POST /api/auth/login). El mismo valor de `identifier` se envía al servicio como usuario y como correo,
    así que el cliente puede entrar con cualquiera de los dos.*/
    @Operation (summary = "Inicia sesión con correo electrónico o número de teléfono y contraseña")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (req == null || req.identifier == null || req.password == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Credenciales incompletas."));
        }
        Client client = clientService.login(req.identifier, req.identifier, req.password);
        if (client == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Usuario o contraseña incorrectos."));
        }

        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(client.getUsername(), req.password.trim())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtGenerator.generateToken(authentication);

        //Los administradores van al panel de habitaciones; los operarios al portal de operador; los clientes, a su perfil.
        String redirectUrl = client.isAdmin()
            ? "/admin/rooms"
            : (client.isOperator() ? "/operator/reservations" : "/profile");
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "token", token,
            "client", client,
            "role", client.getRole(),
            "redirectUrl", redirectUrl
        ));
    }
}
