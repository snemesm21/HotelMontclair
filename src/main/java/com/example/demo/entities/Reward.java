package com.example.demo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad que representa un beneficio o premio disponible en el catálogo de recompensas del hotel.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rewards")
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false)
    private Integer pointsCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LoyaltyTier minimumTier = LoyaltyTier.CLASSIC;

    @Column(length = 500)
    private String imageUrl;

    /**
     * Stock disponible. Si es null, la recompensa tiene disponibilidad ilimitada.
     */
    @Column
    private Integer stock;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @PrePersist
    @PreUpdate
    public void applyDefaults() {
        if (minimumTier == null) {
            minimumTier = LoyaltyTier.CLASSIC;
        }
        if (category == null || category.isBlank()) {
            category = "EXPERIENCIA";
        }
    }

    public boolean hasAvailableStock() {
        return stock == null || stock > 0;
    }
}
