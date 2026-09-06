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
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "services")
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private Double price;

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false, unique = true, length = 40)
    private String tag;

    @Column(nullable = false, length = 80)
    private String schedule;

    @Column(length = 80)
    private String priceLabel;

    // Detailed Card Attributes
    private String heroDescription;
    private String tagline;
    private String headline;
    private String fullDescription;
    private String scheduleNote;
    private String priceNote;
    private String secondaryImageUrl;
    @ElementCollection
    @CollectionTable(name = "service_highlights", joinColumns = @JoinColumn(name = "service_id"))
    private List<Highlight> highlights = new ArrayList<>();
    @ElementCollection
    @CollectionTable(name = "service_gallery_images", joinColumns = @JoinColumn(name = "service_id"))
    private List<String> galleryImages = new ArrayList<>();

    public Service(Long id, String name, String description, Double price, String imageUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
    }

    public Service(Long id, String tag, String name, String description, String schedule, Double price,
            String priceLabel, String imageUrl) {
        this.id = id;
        this.tag = tag;
        this.name = name;
        this.description = description;
        this.schedule = schedule;
        this.price = price;
        this.priceLabel = priceLabel;
        this.imageUrl = imageUrl;
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class Highlight {
        private String title;
        private String description;
    }

    public String getTag() {
        if (tag != null && !tag.isBlank())
            return tag;
        return (id != null) ? String.format("EXPERIENCIA %02d", id) : "EXPERIENCIA";
    }

    public String getSchedule() {
        if (schedule != null && !schedule.isBlank())
            return schedule;
        return "8:00 AM - 10:00 PM";
    }

    public String getPriceLabel() {
        if (priceLabel != null && !priceLabel.isBlank())
            return priceLabel;
        if (price == null || price == 0.0)
            return "Incluido";
        return String.format("Desde € %.0f EUR", price);
    }

    public String getHeroDescription() {
        if (heroDescription != null && !heroDescription.isBlank())
            return heroDescription;
        return description;
    }

    public String getTagline() {
        if (tagline != null && !tagline.isBlank())
            return tagline;
        return getTag() + " · SIGNATURE EXPERIENCE";
    }

    public String getHeadline() {
        if (headline != null && !headline.isBlank())
            return headline;
        return "Una experiencia exclusiva diseñada para ti";
    }

    public String getFullDescription() {
        if (fullDescription != null && !fullDescription.isBlank())
            return fullDescription;
        return description;
    }

    public String getScheduleNote() {
        if (scheduleNote != null && !scheduleNote.isBlank())
            return scheduleNote;
        return "Todos los días";
    }

    public String getPriceNote() {
        if (priceNote != null && !priceNote.isBlank())
            return priceNote;
        return "Por persona · reserva previa";
    }

    public String getSecondaryImageUrl() {
        if (secondaryImageUrl != null && !secondaryImageUrl.isBlank())
            return secondaryImageUrl;
        return imageUrl;
    }

    public void addHighlight(String title, String description) {
        this.highlights.add(new Highlight(title, description));
    }

    public void addGalleryImage(String url) {
        this.galleryImages.add(url);
    }
}