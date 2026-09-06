package com.example.demo.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String number;

    @Column(nullable = false)
    private int floor;

    @Transient
    private Long typeId;

    @ManyToOne
    @JoinColumn(name = "type_id", nullable = false)
    private RoomType type;

    @Column(nullable = false, length = 20)
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
    @ElementCollection
    @CollectionTable(name = "room_highlights", joinColumns = @JoinColumn(name = "room_id"))
    private List<Highlight> highlights = new ArrayList<>();
    @ElementCollection
    @CollectionTable(name = "room_gallery_images", joinColumns = @JoinColumn(name = "room_id"))
    private List<String> galleryImages = new ArrayList<>();

    public Room(Long id, String number, int floor, Long typeId, RoomStatus status) {
        this.id = id;
        this.number = number;
        this.floor = floor;
        this.typeId = typeId;
        this.status = status;
    }

    public Long getTypeId() {
        return typeId != null ? typeId : type == null ? null : type.getId();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class Highlight {
        private String title;
        private String description;
    }

    // Métodos auxiliares
    public void addHighlight(String title, String description) {
        this.highlights.add(new Highlight(title, description));
    }

    public void addGalleryImage(String url) {
        this.galleryImages.add(url);
    }

    // Getters personalizados con lógica de respaldo (fallback)
    public String getHeroDescription() {
        if (heroDescription != null && !heroDescription.isBlank()) return heroDescription;
        return description;
    }

    public String getHeadline() {
        if (headline != null && !headline.isBlank()) return headline;
        return "Una experiencia de descanso excepcional";
    }

    public String getFullDescription() {
        if (fullDescription != null && !fullDescription.isBlank()) return fullDescription;
        return description;
    }

    public String getSecondaryImageUrl() {
        if (secondaryImageUrl != null && !secondaryImageUrl.isBlank()) return secondaryImageUrl;
        return imageUrl;
    }

    public String getPriceLabel() {
        if (pricePerNight <= 0) return "Consultar";
        return String.format("Desde $%.2f € / noche", pricePerNight);
    }
}
