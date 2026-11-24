package com.example.demo.service;

import com.example.demo.dto.SiigoInvoiceDTO;

import java.util.Map;

/**
 * Servicio de integración con Siigo API v1 para la emisión de facturas simuladas
 * (no electrónicas / sin timbre DIAN) de reservas de MontClair Grand Hotel.
 */
public interface SiigoInvoiceService {

    /**
     * Genera o recupera la factura simulada en Siigo para una reserva,
     * utilizando el cliente Consumidor Final (222222222222) y el producto único MONTCLAIR-HOTEL,
     * registrando el nombre completo del huésped y el detalle de su estancia en la descripción.
     */
    SiigoInvoiceDTO generateOrGetInvoice(Long reservationId, boolean forceNew);

    /**
     * Consulta la factura generada previamente en memoria para una reserva, o null si aún no existe.
     */
    SiigoInvoiceDTO getExistingInvoice(Long reservationId);

    /**
     * Verifica la conexión con Siigo API y devuelve los catálogos descubiertos en modo solo lectura.
     */
    Map<String, Object> getStatus();
}
