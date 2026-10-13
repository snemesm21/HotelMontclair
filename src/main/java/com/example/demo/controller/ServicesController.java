package com.example.demo.controller;

import com.example.demo.entities.Service;
import com.example.demo.service.ServiceService;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collection;

@RestController
@RequestMapping({"/api/services"})
@CrossOrigin(origins = "http://localhost:4200")
public class ServicesController {

    @Autowired
    private ServiceService serviceService;

    @Operation (summary = "Obtiene todos los servicios")
    @GetMapping
    public Collection<Service> getAllServices() {
        return serviceService.searchAll();
    }

    @Operation (summary = "Obtiene un servicio por su ID")
    @GetMapping("/{id}")
    public ResponseEntity<Service> getServiceById(@PathVariable Long id) {
        Service svc = serviceService.searchById(id);
        if (svc == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(svc);
    }

    @Operation (summary = "Crea un nuevo servicio")
    @PostMapping
    public ResponseEntity<Service> createService(@RequestBody Service service) {
        service.setId(null);
        serviceService.save(service);
        return ResponseEntity.status(HttpStatus.CREATED).body(service);
    }

    @Operation (summary = "Actualiza un servicio existente por su ID")
    @PutMapping("/{id}")
    public ResponseEntity<Service> updateService(@PathVariable Long id, @RequestBody Service service) {
        service.setId(id);
        serviceService.save(service);
        return ResponseEntity.ok(service);
    }
    @Operation (summary = "Elimina un servicio por su ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        serviceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
