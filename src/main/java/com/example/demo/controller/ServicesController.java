package com.example.demo.controller;

import com.example.demo.entities.Service;
import com.example.demo.repository.ServiceRepository;
import com.example.demo.service.ServiceService;

import io.swagger.v3.oas.annotations.Operation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collection;
import java.util.Optional;

@RestController
@RequestMapping({"/api/services", "/services"})
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS
})
public class ServicesController {

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private ServiceRepository serviceRepository;

    @Operation(summary = "Obtiene todos los servicios (opcionalmente filtrados por hidden)")
    @GetMapping
    public Collection<Service> getAllServices(@RequestParam(required = false) Boolean hidden) {
        if (hidden != null) {
            return serviceService.searchByHidden(hidden);
        }
        return serviceService.searchAll();
    }

    @Operation(summary = "Obtiene únicamente los servicios no ocultos (hidden = false)")
    @GetMapping({"/nothidden", "/not-hidden"})
    public Collection<Service> getNotHiddenServices() {
        return serviceService.searchByHidden(false);
    }

    @Operation(summary = "Obtiene un servicio por su ID")
    @GetMapping("/{id}")
    public ResponseEntity<Service> getServiceById(@PathVariable Long id) {
        Service svc = serviceService.searchById(id);
        if (svc == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(svc);
    }

    @Operation(summary = "Crea un nuevo servicio")
    @PostMapping
    public ResponseEntity<?> createService(@RequestBody Service service) {
        try {
            service.setId(null);
            if (service.getTag() == null || service.getTag().isBlank()) {
                service.setTag("EXPERIENCIA " + (System.currentTimeMillis() % 100000));
            } else {
                Optional<Service> existingTag = serviceRepository.findByTag(service.getTag());
                if (existingTag.isPresent()) {
                    return ResponseEntity.badRequest().body("El tag '" + service.getTag() + "' ya está registrado.");
                }
            }

            if (service.getSchedule() == null || service.getSchedule().isBlank()) {
                service.setSchedule("8:00 AM - 10:00 PM");
            }
            if (service.getPrice() == null) {
                service.setPrice(0.0);
            }
            if (service.getPriceLabel() == null || service.getPriceLabel().isBlank()) {
                service.setPriceLabel(service.getPrice() == 0.0 ? "Incluido" : String.format("Desde € %.0f EUR", service.getPrice()));
            }
            if (service.getHeroDescription() == null || service.getHeroDescription().isBlank()) {
                service.setHeroDescription(service.getDescription());
            }
            if (service.getFullDescription() == null || service.getFullDescription().isBlank()) {
                service.setFullDescription(service.getDescription());
            }
            if (service.getSecondaryImageUrl() == null || service.getSecondaryImageUrl().isBlank()) {
                service.setSecondaryImageUrl(service.getImageUrl());
            }

            Service saved = serviceService.save(service);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al crear el servicio: " + e.getMessage());
        }
    }

    @Operation(summary = "Actualiza un servicio existente por su ID")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateService(@PathVariable Long id, @RequestBody Service service) {
        try {
            Service existing = serviceService.searchById(id);
            if (existing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Servicio no encontrado con id: " + id);
            }

            // Validar que el tag no pertenezca a otro servicio distinto
            if (service.getTag() != null && !service.getTag().isBlank()) {
                Optional<Service> existingTag = serviceRepository.findByTag(service.getTag());
                if (existingTag.isPresent() && !existingTag.get().getId().equals(id)) {
                    return ResponseEntity.badRequest().body("El tag '" + service.getTag() + "' ya está en uso por otro servicio.");
                }
                existing.setTag(service.getTag());
            }

            if (service.getName() != null && !service.getName().isBlank()) {
                existing.setName(service.getName());
            }
            if (service.getDescription() != null && !service.getDescription().isBlank()) {
                existing.setDescription(service.getDescription());
            }
            if (service.getPrice() != null) {
                existing.setPrice(service.getPrice());
                if (service.getPriceLabel() != null && !service.getPriceLabel().isBlank()) {
                    existing.setPriceLabel(service.getPriceLabel());
                } else {
                    existing.setPriceLabel(existing.getPrice() == 0.0 ? "Incluido" : String.format("Desde € %.0f EUR", existing.getPrice()));
                }
            } else if (service.getPriceLabel() != null && !service.getPriceLabel().isBlank()) {
                existing.setPriceLabel(service.getPriceLabel());
            }

            if (service.getImageUrl() != null && !service.getImageUrl().isBlank()) {
                existing.setImageUrl(service.getImageUrl());
            }
            if (service.getSchedule() != null && !service.getSchedule().isBlank()) {
                existing.setSchedule(service.getSchedule());
            }
            if (service.getIcon() != null) {
                existing.setIcon(service.getIcon());
            }
            if (service.getHeroDescription() != null && !service.getHeroDescription().isBlank()) {
                existing.setHeroDescription(service.getHeroDescription());
            }
            if (service.getTagline() != null && !service.getTagline().isBlank()) {
                existing.setTagline(service.getTagline());
            }
            if (service.getHeadline() != null && !service.getHeadline().isBlank()) {
                existing.setHeadline(service.getHeadline());
            }
            if (service.getFullDescription() != null && !service.getFullDescription().isBlank()) {
                existing.setFullDescription(service.getFullDescription());
            }
            if (service.getScheduleNote() != null && !service.getScheduleNote().isBlank()) {
                existing.setScheduleNote(service.getScheduleNote());
            }
            if (service.getPriceNote() != null && !service.getPriceNote().isBlank()) {
                existing.setPriceNote(service.getPriceNote());
            }
            if (service.getSecondaryImageUrl() != null && !service.getSecondaryImageUrl().isBlank()) {
                existing.setSecondaryImageUrl(service.getSecondaryImageUrl());
            }
            if (service.getHighlights() != null && !service.getHighlights().isEmpty()) {
                existing.getHighlights().clear();
                existing.getHighlights().addAll(service.getHighlights());
            }
            if (service.getGalleryImages() != null && !service.getGalleryImages().isEmpty()) {
                existing.getGalleryImages().clear();
                existing.getGalleryImages().addAll(service.getGalleryImages());
            }
            existing.setHidden(service.isHidden());

            Service updated = serviceService.save(existing);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al actualizar el servicio: " + e.getMessage());
        }
    }

    @Operation(summary = "Crea o actualiza un servicio")
    @PutMapping
    public ResponseEntity<?> createOrUpdateService(@RequestBody Service service) {
        if (service.getId() != null) {
            return updateService(service.getId(), service);
        }
        return createService(service);
    }

    @Operation(summary = "Elimina un servicio por su ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        serviceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
