package com.example.demo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Libro mayor (ledger) de movimientos de puntos de un cliente:
 * adiciones por estadías/bonos, restas por ajustes y canjes de recompensas.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "point_transactions")
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PointTransactionType type;

    /**
     * Variación de puntos: positivo para adiciones, negativo para restas y canjeos.
     */
    @Column(nullable = false)
    private Integer pointsDelta;

    /**
     * Saldo disponible del cliente inmediatamente después de aplicar este movimiento.
     */
    @Column(nullable = false)
    private Integer balanceAfter;

    @Column(nullable = false, length = 300)
    private String description;

    @Column
    private Long referenceId;

    @Column(length = 40)
    private String referenceType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void applyDefaults() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
