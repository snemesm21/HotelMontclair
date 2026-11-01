package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.service.ClientService;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

//Controlador REST de clientes (/api/clients).
@RestController
@RequestMapping({"/api/clients"})
@CrossOrigin(origins = "http://localhost:4200")
public class ClientController {

    @Autowired
    private ClientService clientService;

    //Lista todos los clientes (GET /api/clients).
    @Operation (summary = "Obtiene todos los clientes")
    @GetMapping
    public List<Client> getAllClients() {
        return clientService.findAll();
    }

    //Busca un cliente por id (GET /api/clients/{id}).
    @Operation (summary = "Obtiene un cliente por su ID")
    @GetMapping("/{id}")
    public ResponseEntity<Client> getClientById(@PathVariable Long id) {
        Client client = clientService.findById(id);
        if (client == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(client);
    }

    //Crea un cliente (POST /api/clients). Se pone el id en null para que la base de datos genere uno nuevo.
    @Operation (summary = "Crea un nuevo cliente")
    @PostMapping
    public ResponseEntity<Client> createClient(@RequestBody Client client) {
        client.setId(null);
        Client saved = clientService.save(client);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    //Actualiza un cliente (PUT /api/clients/{id}). El id de la URL manda sobre el del cuerpo. Reemplaza todos los campos del cliente.
    @Operation (summary = "Actualiza un cliente existente por su ID")
    @PutMapping("/{id}")
    public ResponseEntity<Client> updateClient(@PathVariable Long id, @RequestBody Client client) {
        client.setId(id);
        Client updated = clientService.save(client);
        return ResponseEntity.ok(updated);
    }

    //Elimina un cliente (DELETE /api/clients/{id}).
    @Operation (summary = "Elimina un cliente por su ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
