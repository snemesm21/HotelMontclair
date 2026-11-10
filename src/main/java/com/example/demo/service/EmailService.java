package com.example.demo.service;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.entities.Client;

/**
 * Interfaz del servicio de notificaciones por correo electrónico (Resend).
 */
public interface EmailService {

    /**
     * Envía un correo en formato HTML a través de la API de Resend.
     *
     * @param to          Correo electrónico del destinatario.
     * @param subject     Asunto del correo.
     * @param htmlContent Cuerpo HTML del mensaje.
     * @return true si Resend aceptó el envío (HTTP 2xx), false si está deshabilitado o falló.
     */
    boolean sendHtmlEmail(String to, String subject, String htmlContent);

    /**
     * Envía el correo de bienvenida cuando se registra un nuevo huésped.
     */
    void sendWelcomeEmail(Client client);

    /**
     * Envía la confirmación de una nueva reserva creada.
     */
    void sendReservationCreatedEmail(ReservationDTO reservation);

    /**
     * Envía la notificación de actualización de una reserva existente.
     */
    void sendReservationUpdatedEmail(ReservationDTO reservation);

    /**
     * Envía la confirmación de cancelación de una reserva.
     */
    void sendReservationCancelledEmail(ReservationDTO reservation);

    /**
     * Envía el comprobante de pago cuando se liquida la cuenta de la estancia.
     */
    void sendPaymentReceiptEmail(ReservationDTO reservation);

    /**
     * Envía el resumen final de cierre de estancia (Check-out).
     */
    void sendStayCompletedEmail(ReservationDTO reservation);
}

