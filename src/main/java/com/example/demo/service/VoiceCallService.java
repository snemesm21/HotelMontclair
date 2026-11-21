package com.example.demo.service;

import com.example.demo.dto.ReservationDTO;

import java.util.Map;

/**
 * Servicio de Agente de Voz Conversacional para realizar llamadas telefónicas
 * salientes automáticas cuando se confirma una reserva en MontClair Grand Hotel.
 */
public interface VoiceCallService {

    /**
     * Resultado estructurado de un intento de llamada saliente del agente de voz.
     */
    record VoiceCallResult(
            boolean success,
            String provider,
            String callId,
            String toPhone,
            String message
    ) {
    }

    /**
     * Dispara automáticamente la llamada telefónica de confirmación de reserva al teléfono del cliente.
     */
    VoiceCallResult triggerReservationConfirmationCall(ReservationDTO reservation);

    /**
     * Dispara la llamada telefónica de confirmación de una reserva permitiendo sobreescribir el teléfono destino.
     */
    VoiceCallResult triggerReservationConfirmationCall(ReservationDTO reservation, String overridePhoneNumber);

    /**
     * Dispara una llamada de prueba interactiva a cualquier número telefónico con datos de ejemplo y el contexto real del hotel.
     */
    VoiceCallResult triggerTestCall(String phoneNumber, String guestName);

    /**
     * Construye el System Prompt conversacional completo con los datos de la reserva y el contexto en vivo de la BD.
     */
    String buildReservationAgentPrompt(ReservationDTO reservation);

    /**
     * Construye el saludo inicial hablado por el agente al contestar la llamada.
     */
    String buildFirstSentence(ReservationDTO reservation);

    /**
     * Normaliza un número telefónico a formato internacional E.164 (ej. +573001234567).
     */
    String normalizePhoneNumber(String rawPhone);

    /**
     * Devuelve el estado actual de la configuración del agente de voz.
     */
    Map<String, Object> getVoiceAgentStatus();

    /**
     * Genera una respuesta conversacional optimizada para voz telefónica en tiempo real (<20ms),
     * con conocimiento de la reserva activa del huésped, del catálogo en vivo de la BD y capacidad
     * de agregar servicios adicionales por voz durante la llamada.
     */
    String generateVoiceConversationReply(
            Long reservationId,
            String fallbackGuestName,
            String fallbackRoomType,
            long fallbackNights,
            double fallbackTotal,
            String userSpeech);
}
