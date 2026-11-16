package com.example.demo.service;

import java.util.Map;

/**
 * Servicio para el Asistente IA Concierge de MontClair Grand Hotel.
 * Centraliza la extracción de datos en tiempo real de la base de datos
 * (habitaciones, tarifas, servicios y disponibilidad) y la orquestación con n8n o motor inteligente.
 */
public interface AiAssistantService {

    /**
     * Procesa un mensaje de usuario utilizando el contexto en vivo del hotel en la base de datos.
     * @param userMessage Mensaje o consulta enviada por el huésped
     * @param sessionId Identificador de sesión para seguimiento de conversación
     * @return Respuesta generada con base en los datos reales del hotel
     */
    String generateResponse(String userMessage, String sessionId);

    /**
     * Obtiene un mapa estructurado con todo el contexto en vivo del hotel
     * (habitaciones disponibles, tipos de habitación, servicios vigentes y políticas).
     * @return Mapa con la información en tiempo real
     */
    Map<String, Object> getHotelLiveContext();

    /**
     * Formatea el contexto en vivo del hotel en texto legible para prompts de IA o n8n.
     * @return Cadena con el contexto completo
     */
    String getFormattedHotelContext();
}

