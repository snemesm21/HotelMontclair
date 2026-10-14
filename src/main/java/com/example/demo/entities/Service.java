package com.example.demo.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "services")
@JsonIgnoreProperties(ignoreUnknown = true)
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

    @Column(length = 1000)
    private String heroDescription;

    @Column(length = 100)
    private String tagline;

    @Column(length = 200)
    private String headline;

    @Column(length = 2000)
    private String fullDescription;

    @Column(length = 100)
    private String scheduleNote;

    @Column(length = 100)
    private String priceNote;

    @Column(length = 500)
    private String secondaryImageUrl;

    @Column(length = 100)
    private String icon;

    @ElementCollection
    @CollectionTable(name = "service_highlights", joinColumns = @JoinColumn(name = "service_id"))
    @Builder.Default
    private List<Highlight> highlights = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "service_gallery_images", joinColumns = @JoinColumn(name = "service_id"))
    @Builder.Default
    private List<String> galleryImages = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean hidden = false;

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

    @Embeddable
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Highlight {
        private String title;
        private String description;

        public Highlight() {
        }

        public Highlight(String title, String description) {
            this.title = title;
            this.description = description;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public void addHighlight(String title, String description) {
        if (this.highlights == null) {
            this.highlights = new ArrayList<>();
        }
        this.highlights.add(new Highlight(title, description));
    }

    public void addGalleryImage(String url) {
        if (this.galleryImages == null) {
            this.galleryImages = new ArrayList<>();
        }
        this.galleryImages.add(url);
    }
}