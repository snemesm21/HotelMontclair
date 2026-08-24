package com.example.demo.entities;

import java.util.ArrayList;
import java.util.List;

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

    public Service() {}

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

    public static class Highlight {
        private String title;
        private String description;

        public Highlight() {}
        public Highlight(String title, String description) {
            this.title = title;
            this.description = description;
        }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getTag() {
        if (tag != null && !tag.isBlank()) return tag;
        return (id != null) ? String.format("EXPERIENCIA %02d", id) : "EXPERIENCIA";
    }
    public void setTag(String tag) { this.tag = tag; }

    public String getSchedule() {
        if (schedule != null && !schedule.isBlank()) return schedule;
        return "8:00 AM - 10:00 PM";
    }
    public void setSchedule(String schedule) { this.schedule = schedule; }

    public String getPriceLabel() {
        if (priceLabel != null && !priceLabel.isBlank()) return priceLabel;
        if (price == null || price == 0.0) return "Incluido";
        return String.format("Desde € %.0f EUR", price);
    }
    public void setPriceLabel(String priceLabel) { this.priceLabel = priceLabel; }

    public String getHeroDescription() {
        if (heroDescription != null && !heroDescription.isBlank()) return heroDescription;
        return description;
    }
    public void setHeroDescription(String heroDescription) { this.heroDescription = heroDescription; }

    public String getTagline() {
        if (tagline != null && !tagline.isBlank()) return tagline;
        return getTag() + " · SIGNATURE EXPERIENCE";
    }
    public void setTagline(String tagline) { this.tagline = tagline; }

    public String getHeadline() {
        if (headline != null && !headline.isBlank()) return headline;
        return "Una experiencia exclusiva diseñada para ti";
    }
    public void setHeadline(String headline) { this.headline = headline; }

    public String getFullDescription() {
        if (fullDescription != null && !fullDescription.isBlank()) return fullDescription;
        return description;
    }
    public void setFullDescription(String fullDescription) { this.fullDescription = fullDescription; }

    public String getScheduleNote() {
        if (scheduleNote != null && !scheduleNote.isBlank()) return scheduleNote;
        return "Todos los días";
    }
    public void setScheduleNote(String scheduleNote) { this.scheduleNote = scheduleNote; }

    public String getPriceNote() {
        if (priceNote != null && !priceNote.isBlank()) return priceNote;
        return "Por persona · reserva previa";
    }
    public void setPriceNote(String priceNote) { this.priceNote = priceNote; }

    public String getSecondaryImageUrl() {
        if (secondaryImageUrl != null && !secondaryImageUrl.isBlank()) return secondaryImageUrl;
        return imageUrl;
    }
    public void setSecondaryImageUrl(String secondaryImageUrl) { this.secondaryImageUrl = secondaryImageUrl; }

    public List<Highlight> getHighlights() { return highlights; }
    public void setHighlights(List<Highlight> highlights) { this.highlights = highlights; }

    public List<String> getGalleryImages() { return galleryImages; }
    public void setGalleryImages(List<String> galleryImages) { this.galleryImages = galleryImages; }

    public void addHighlight(String title, String description) {
        this.highlights.add(new Highlight(title, description));
    }

    public void addGalleryImage(String url) {
        this.galleryImages.add(url);
    }
}