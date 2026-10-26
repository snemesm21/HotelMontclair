package com.example.demo.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) para la gestión y visualización de reservas.
 * Evita devolver entidades JPA con objetos anidados cíclicos o dependientes de sesión de Hibernate.
 * Proporciona campos planos para clientes, habitaciones, totales calculados,
 * el nuevo estado de estancia (CONFIRMED, ACTIVE, INACTIVE, CANCELLED) y
 * el estado financiero de la cuenta (paid / pendingBalance).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDTO {

    private Long id;

    // Datos del cliente
    private Long clientId;
    private String clientFirstName;
    private String clientLastName;
    private String clientEmail;
    private String clientPhone;

    // Datos de la habitación
    private Long roomId;
    private String roomNumber;
    private String roomName;
    private String roomTypeName;
    private Long roomTypeId;

    private double pricePerNight;

    // Fechas y ocupación
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private int numberOfPeople;

    // Estados de ciclo de vida y financiero
    // Estados: CONFIRMED, ACTIVE (huésped en estancia), INACTIVE (estancia finalizada), CANCELLED
    private String status;

    // Indicador de cuenta pagada (true: cuenta limpia / pagada, false: saldo pendiente)
    private boolean paid;

    // Cálculos de noches y precios
    private long nights;
    private double roomSubtotal;
    private double servicesSubtotal;
    private double totalPrice;

    // Saldo pendiente de pago (0.0 si paid es true; igual a totalPrice si aún no se paga)
    private double pendingBalance;

    // Lista plana de servicios adicionales
    @Builder.Default
    private List<ServiceDetail> services = new ArrayList<>();

    // Sub-estructuras livianas para compatibilidad con vistas detalladas
    private ClientSummary client;
    @Builder.Default
    private List<ReservationRoomSummary> reservationRooms = new ArrayList<>();

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceDetail {
        private Long id;
        private String name;
        private int quantity;
        private double unitPrice;
        private double subtotal;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientSummary {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomSummary {
        private Long id;
        private String number;
        private String name;
        private double pricePerNight;
        private RoomTypeSummary roomType;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomTypeSummary {
        private Long id;
        private String name;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationRoomSummary {
        private Long id;
        private double pricePerNight;
        private RoomSummary room;
        @Builder.Default
        private List<AcquiredServiceSummary> acquiredServices = new ArrayList<>();
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AcquiredServiceSummary {
        private Long id;
        private int quantity;
        private double unitPrice;
        private ServiceItemSummary service;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceItemSummary {
        private Long id;
        private String name;
    }
}
