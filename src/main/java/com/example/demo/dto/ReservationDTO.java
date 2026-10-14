
package com.example.demo.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDTO {

    private Long id;

    private String clientFirstName;
    private String clientLastName;
    private String clientEmail;
    private String clientPhone;

    private String roomNumber;
    private String roomName;
    private String roomTypeName;

    private double pricePerNight;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;

    private int numberOfPeople;
    private String status;

    private long nights;
    private double roomSubtotal;
    private double servicesSubtotal;
    private double totalPrice;

    @Builder.Default
    private List<ServiceDetail> services = new ArrayList<>();

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
}
