package com.example.demo.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(exclude = {"reservation", "acquiredServices"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reserva_habitaciones")
public class ReservationRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reservation reservation;

    @ManyToOne
    @JoinColumn(name = "habitacion_id", nullable = false)
    private Room room;

    @Column(name = "precio_por_noche_registrado", nullable = false)
    private double pricePerNight;

    @OneToMany(mappedBy = "reservationRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AcquiredService> acquiredServices = new ArrayList<>();

    public void addAcquiredService(AcquiredService service) {
        acquiredServices.add(service);
        service.setReservationRoom(this);
    }

    public void removeAcquiredService(AcquiredService service) {
        acquiredServices.remove(service);
        service.setReservationRoom(null);
    }
}
