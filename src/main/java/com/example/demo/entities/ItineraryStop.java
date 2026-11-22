package com.example.demo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "itinerary_stops", uniqueConstraints = {
        @UniqueConstraint(name = "uk_itinerary_client_place", columnNames = {"client_id", "place_id"})
})
public class ItineraryStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private TouristPlace place;

    @Column(nullable = false, updatable = false)
    private LocalDateTime addedAt;

    public ItineraryStop(Client client, TouristPlace place) {
        this.client = client;
        this.place = place;
        this.addedAt = LocalDateTime.now();
    }
}
