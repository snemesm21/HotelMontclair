package com.example.demo.service;

import com.example.demo.entities.Service;
import com.example.demo.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;

@org.springframework.stereotype.Service
public class ServicesServiceImpl implements ServiceService {

    @Autowired
    private ServiceRepository repo;

    @Autowired
    private com.example.demo.repository.AcquiredServiceRepository acquiredServiceRepo;

    @Override
    public Service searchById(Long id) {
        if (id == null) return null;
        return repo.findById(id).orElse(null);
    }

    @Override
    public Collection<Service> searchAll() {
        return repo.findAll();
    }

    @Override
    public Collection<Service> searchByHidden(boolean hidden) {
        return repo.findByHidden(hidden);
    }

    @Override
    @Transactional
    public Service save(Service service) {
        if (service.getTag() == null || service.getTag().isBlank()) {
            service.setTag("EXPERIENCIA " + (System.currentTimeMillis() % 100000));
        } else {
            Optional<Service> existingTag = repo.findByTag(service.getTag());
            if (existingTag.isPresent() && (service.getId() == null || !existingTag.get().getId().equals(service.getId()))) {
                throw new IllegalArgumentException("El tag '" + service.getTag() + "' ya está registrado.");
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

        return repo.save(service);
    }

    @Override
    @Transactional
    public Service update(Long id, Service service) {
        Service existing = searchById(id);
        if (existing == null) {
            return null;
        }

        if (service.getTag() != null && !service.getTag().isBlank()) {
            Optional<Service> existingTag = repo.findByTag(service.getTag());
            if (existingTag.isPresent() && !existingTag.get().getId().equals(id)) {
                throw new IllegalArgumentException("El tag '" + service.getTag() + "' ya está en uso por otro servicio.");
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

        return repo.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (id == null || !repo.existsById(id)) {
            throw new com.example.demo.errors.NotFoundException(id);
        }
        if (acquiredServiceRepo.existsByService_Id(id)) {
            throw new IllegalArgumentException("No se puede eliminar el servicio porque está asociado a una reservación.");
        }
        repo.deleteById(id);
    }
}