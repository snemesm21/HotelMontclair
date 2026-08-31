package com.example.demo.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Service {

    private Long id;
    private String name;
    private String description;
    private Double price;
    private String imageUrl;
    private String tag;
    private String schedule;
    private String priceLabel;

    // Detailed Card Attributes
    private String heroDescription;
    private String tagline;
    private String headline;
    private String fullDescription;
    private String scheduleNote;
    private String priceNote;
    private String secondaryImageUrl;
    private List<Highlight> highlights = new ArrayList<>();
    private List<String> galleryImages = new ArrayList<>();

    public Service(Long id, String name, String description, Double price, String imageUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
    }

    public Service(Long id, String tag, String name, String description, String schedule, Double price, String priceLabel, String imageUrl) {
        this.id = id;
        this.tag = tag;
        this.name = name;
        this.description = description;
        this.schedule = schedule;
        this.price = price;
        this.priceLabel = priceLabel;
        this.imageUrl = imageUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Highlight {
        private String title;
        private String description;
    }

    // Getters con lógica personalizada (fallback)
    public String getTag() {
        if (tag != null && !tag.isBlank()) return tag;
        return (id != null) ? String.format("EXPERIENCIA %02d", id) : "EXPERIENCIA";
    }

    public String getSchedule() {
        if (schedule != null && !schedule.isBlank()) return schedule;
        return "8:00 AM - 10:00 PM";
    }

    public String getPriceLabel() {
        if (priceLabel != null && !priceLabel.isBlank()) return priceLabel;
        if (price == null || price == 0.0) return "Incluido";
        return String.format("Desde € %.0f EUR", price);
    }

    public String getHeroDescription() {
        if (heroDescription != null && !heroDescription.isBlank()) return heroDescription;
        return description;
    }

    public String getTagline() {
        if (tagline != null && !tagline.isBlank()) return tagline;
        return getTag() + " · SIGNATURE EXPERIENCE";
    }

    public String getHeadline() {
        if (headline != null && !headline.isBlank()) return headline;
        return "Una experiencia exclusiva diseñada para ti";
    }

    public String getFullDescription() {
        if (fullDescription != null && !fullDescription.isBlank()) return fullDescription;
        return description;
    }

    public String getScheduleNote() {
        if (scheduleNote != null && !scheduleNote.isBlank()) return scheduleNote;
        return "Todos los días";
    }

    public String getPriceNote() {
        if (priceNote != null && !priceNote.isBlank()) return priceNote;
        return "Por persona · reserva previa";
    }

    public String getSecondaryImageUrl() {
        if (secondaryImageUrl != null && !secondaryImageUrl.isBlank()) return secondaryImageUrl;
        return imageUrl;
    }

    public void addHighlight(String title, String description) {
        this.highlights.add(new Highlight(title, description));
    }

    public void addGalleryImage(String url) {
        this.galleryImages.add(url);
    }
}