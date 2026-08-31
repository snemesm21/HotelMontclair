package com.example.demo.entities;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private Long id;
    private String number;
    private int floor;
    private Long typeId;
    private RoomStatus status;

    // Atributos visuales (tarjeta)
    private String name;
    private int capacity;
    private String bedType;
    private int area;
    private String imageUrl;
    private String description;
    private double pricePerNight;

    // Atributos de detalle
    private String heroDescription;
    private String headline;
    private String fullDescription;
    private String secondaryImageUrl;
    private List<Highlight> highlights = new ArrayList<>();
    private List<String> galleryImages = new ArrayList<>();

    public Room() {}

    public Room(Long id, String number, int floor, Long typeId, RoomStatus status) {
        this.id = id;
        this.number = number;
        this.floor = floor;
        this.typeId = typeId;
        this.status = status;
    }

    // Clase interna para highlights (mismo patrón que Service)
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

    // Métodos auxiliares
    public void addHighlight(String title, String description) {
        this.highlights.add(new Highlight(title, description));
    }

    public void addGalleryImage(String url) {
        this.galleryImages.add(url);
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public int getFloor() { return floor; }
    public void setFloor(int floor) { this.floor = floor; }

    public Long getTypeId() { return typeId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }

    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public String getBedType() { return bedType; }
    public void setBedType(String bedType) { this.bedType = bedType; }

    public int getArea() { return area; }
    public void setArea(int area) { this.area = area; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(double pricePerNight) { this.pricePerNight = pricePerNight; }

    public String getHeroDescription() {
        if (heroDescription != null && !heroDescription.isBlank()) return heroDescription;
        return description;
    }
    public void setHeroDescription(String heroDescription) { this.heroDescription = heroDescription; }

    public String getHeadline() {
        if (headline != null && !headline.isBlank()) return headline;
        return "Una experiencia de descanso excepcional";
    }
    public void setHeadline(String headline) { this.headline = headline; }

    public String getFullDescription() {
        if (fullDescription != null && !fullDescription.isBlank()) return fullDescription;
        return description;
    }
    public void setFullDescription(String fullDescription) { this.fullDescription = fullDescription; }

    public String getSecondaryImageUrl() {
        if (secondaryImageUrl != null && !secondaryImageUrl.isBlank()) return secondaryImageUrl;
        return imageUrl;
    }
    public void setSecondaryImageUrl(String secondaryImageUrl) { this.secondaryImageUrl = secondaryImageUrl; }

    public List<Highlight> getHighlights() { return highlights; }
    public void setHighlights(List<Highlight> highlights) { this.highlights = highlights; }

    public List<String> getGalleryImages() { return galleryImages; }
    public void setGalleryImages(List<String> galleryImages) { this.galleryImages = galleryImages; }

    public String getPriceLabel() {
        if (pricePerNight <= 0) return "Consultar";
        return String.format("Desde $%.2f € / noche", pricePerNight);
    }
}
