package com.example.demo.controller;

import com.example.demo.dto.ClientDTO;
import com.example.demo.entities.Client;
import com.example.demo.mapper.ClientMapper;
import com.example.demo.service.ClientService;
import com.example.demo.service.JwtService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;

//Controlador REST de clientes (/api/clients).
@RestController
@RequestMapping({ "/api/clients" })
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class ClientController {

    @Autowired
    private ClientService clientService;

    @Autowired(required = false)
    private JwtService jwtService;

    @Value("${hotelmontclair.auth.cookie-secure:false}")
    private boolean cookieSecure;

    @Operation(summary = "Obtiene todos los clientes")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ClientDTO> getAllClients() {
        return clientService.findAll().stream()
                .map(ClientMapper.INSTANCE::convert)
                .toList();
    }

    // Misma URL para todos: el cliente se identifica por el JWT, no por un id en la URL
    @Operation(summary = "Obtiene el cliente autenticado a partir de su JWT")
    @GetMapping("/details")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "No autenticado"));
        }
        Client client = null;
        if (auth.getPrincipal() instanceof JwtService.JwtClaims claims) {
            client = clientService.findById(claims.clientId());
        } else if (auth.getName() != null) {
            client = clientService.findByUsername(auth.getName());
        }
        if (client == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Cliente no encontrado"));
        }
        return ResponseEntity.ok(ClientMapper.INSTANCE.convert(client));
    }

    @Operation(summary = "Obtiene un cliente por su ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','CLIENT')")
    public ResponseEntity<ClientDTO> getClientById(@PathVariable Long id) {
        if (!canAccessClient(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Client client = clientService.findById(id);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ClientMapper.INSTANCE.convert(client));
    }

    @Operation(summary = "Crea un nuevo cliente")
    @PostMapping
    public ResponseEntity<ClientDTO> createClient(@RequestBody Client client) {
        client.setId(null);
        Client saved = clientService.save(client);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdminSession = authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(a -> "ADMIN".equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdminSession && jwtService != null && saved != null && saved.getId() != null) {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletResponse response = attrs != null ? attrs.getResponse() : null;
            if (response != null) {
                String jwt = jwtService.createToken(saved.getId(), saved.getEmail(), saved.getRole());
                ResponseCookie cookie = ResponseCookie.from("HM_SESSION", jwt)
                        .httpOnly(true).secure(cookieSecure).sameSite("Lax")
                        .path("/").maxAge(7200).build();
                response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(ClientMapper.INSTANCE.convert(saved));
    }

    @Operation(summary = "Actualiza un cliente existente por su ID")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','CLIENT')")
    public ResponseEntity<ClientDTO> updateClient(@PathVariable Long id, @RequestBody Client client) {
        if (!canAccessClient(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        client.setId(id);
        Client updated = clientService.save(client);
        return ResponseEntity.ok(ClientMapper.INSTANCE.convert(updated));
    }

    @Operation(summary = "Elimina un cliente por su ID")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // El personal accede a cualquiera; un cliente solo a su propio registro
    private boolean canAccessClient(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtService.JwtClaims claims)) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(claims.role()) || "OPERATOR".equalsIgnoreCase(claims.role())
                || "OPERARIO".equalsIgnoreCase(claims.role())) {
            return true;
        }
        return claims.clientId().equals(id);
    }
}
