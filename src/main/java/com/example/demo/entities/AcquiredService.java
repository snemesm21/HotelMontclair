package com.example.demo.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Getter
@Setter
@ToString(exclude = "reservationRoom")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "servicios_adquiridos")
public class AcquiredService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reserva_habitacion_id", nullable = false)
    private ReservationRoom reservationRoom;

    @ManyToOne
    @JoinColumn(name = "servicio_id", nullable = false)
    private Service service;

    @Column(name = "fecha", nullable = false)
    private LocalDate date;

    @Column(name = "cantidad", nullable = false)
    private int quantity;

    @Column(name = "precio_unitario", nullable = false)
    private double unitPrice;

    public double getSubtotal() {
        return this.quantity * this.unitPrice;
    }
}
