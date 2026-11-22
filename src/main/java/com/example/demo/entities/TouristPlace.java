package com.example.demo.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tourist_places", uniqueConstraints = {
        @UniqueConstraint(name = "uk_tourist_places_slug", columnNames = "slug")
})
public class TouristPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String slug;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "description", nullable = false, length = 3000)
    private String longDescription;

    @Column(nullable = false, length = 500)
    private String shortDescription;

    @Column(nullable = false, length = 80)
    private String category;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tourist_place_images", joinColumns = @JoinColumn(name = "place_id"))
    @Column(name = "image_url", nullable = false, length = 1000)
    private List<String> imageUrls = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tourist_place_image_attributions", joinColumns = @JoinColumn(name = "place_id"))
    @Column(name = "attribution", nullable = false, length = 2000)
    private List<String> imageAttributions = new ArrayList<>();

    @Column(nullable = false, length = 300)
    private String address;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private int suggestedDurationMinutes;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true;

    @Transient
    public String getDescription() {
        return longDescription;
    }

    @Transient
    public void setDescription(String description) {
        this.longDescription = description;
    }
}
