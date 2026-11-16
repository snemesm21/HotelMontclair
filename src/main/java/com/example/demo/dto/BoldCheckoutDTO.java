package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO con los parámetros firmados requeridos por el widget oficial BoldCheckout
 * (https://checkout.bold.co/library/boldPaymentButton.js).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoldCheckoutDTO {

    private Long reservationId;
    private String orderId;
    private String apiKey;
    private String amount;
    private String currency;
    private String integritySignature;
    private String description;
    private String redirectionUrl;
    private String customerEmail;
    private String customerFullName;
    private String customerPhone;
}

