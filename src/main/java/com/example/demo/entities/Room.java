
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(exclude = "roomType")
@Builder
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
    private RoomType roomType;

    @Column(nullable = false, length = 20)
    private RoomStatus status;

    @Column(length = 100)
    private String name;

    private int capacity;

    @Column(length = 50)
    private String bedType;

    private int area;

    private double pricePerNight;

    @Column(length = 1000)
    private String heroDescription;

    @Column(length = 200)
    private String headline;

    @Column(length = 2000)
    private String fullDescription;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 500)
    private String secondaryImageUrl;

    @Column(length = 500)
    private String description;

    @ElementCollection
    @CollectionTable(name = "room_highlights", joinColumns = @JoinColumn(name = "room_id"))
    @Builder.Default
    private List<Highlight> highlights = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "room_gallery_images", joinColumns = @JoinColumn(name = "room_id"))
    @Builder.Default
    private List<String> galleryImages = new ArrayList<>();

    public Room(Long id, String number, int floor, Long typeId, RoomStatus status) {
        this.id = id;
        this.number = number;
        this.floor = floor;
        this.typeId = typeId;
        this.status = status;
        this.highlights = new ArrayList<>();
        this.galleryImages = new ArrayList<>();
    }

    public Long getTypeId() {
        return typeId != null
                ? typeId
                : (roomType != null ? roomType.getId() : null);
    }

    public RoomType getType() {
        return roomType;
    }

    public void setType(RoomType type) {
        this.roomType = type;
    }

    @Embeddable
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
