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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString(exclude = "reservationRooms")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reservas")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Client client;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "cantidad_personas", nullable = false)
    private int numberOfPeople;

    @Column(name = "estado", nullable = false, length = 20)
    private String status;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReservationRoom> reservationRooms = new ArrayList<>();
    
    public void addReservationRoom(ReservationRoom room) {
        reservationRooms.add(room);
        room.setReservation(this);
    }
    
    public void removeReservationRoom(ReservationRoom room) {
        reservationRooms.remove(room);
        room.setReservation(null);
    }
}
