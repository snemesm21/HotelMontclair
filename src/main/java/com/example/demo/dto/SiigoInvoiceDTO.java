package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de respuesta para la factura simulada generada en Siigo Nube
 * bajo la modalidad Consumidor Final (222222222222) y producto único MONTCLAIR-HOTEL,
 * sin envío a la DIAN (stamp.send = false).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiigoInvoiceDTO {

    private boolean success;
    private boolean liveSiigo;
    private boolean dianSent;

    private Long reservationId;
    private String siigoInvoiceId;
    private Long documentId;
    private String documentTypeName;
    private Long invoiceNumber;
    private String invoiceName;
    private String issueDate;

    // Cliente Consumidor Final en Siigo + Nombre del huésped de la reserva
    private String customerIdentification;
    private String customerSiigoName;
    private String guestFullName;
    private String guestEmail;
    private String guestPhone;

    // Producto único en Siigo
    private String productCode;
    private String productName;
    private String itemDescription;

    // Totales y forma de pago
    private double roomSubtotal;
    private double servicesSubtotal;
    private double totalAmount;
    private String currency;
    private String paymentMethodName;

    private String observations;
    private String publicUrl;
    private String message;
}
