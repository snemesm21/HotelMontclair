package com.example.demo.controller;

import com.example.demo.entities.Room;
import com.example.demo.entities.RoomStatus;
import com.example.demo.entities.Service;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.ServiceRepository;
import com.example.demo.service.AiAssistantService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para el Asistente IA de MontClair Grand Hotel (/api/ai).
 * Conecta el chat frontend y los agentes externos (n8n, telefonía) directamente con los datos reales
 * de la base de datos (habitaciones disponibles, servicios, tarifas y políticas).
 */
@Tag(name = "AI Concierge", description = "Endpoints para el agente de IA y asistente virtual conectado a la BD")
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class AiController {

    @Autowired
    private AiAssistantService aiAssistantService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    public static class ChatRequest {
        public String message;
        public String sessionId;

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }

    /**
     * Procesa un mensaje del chat en vivo utilizando el contexto en tiempo real de la base de datos.
     */
    @Operation(summary = "Conversa con el Agente Concierge IA con datos en vivo de la BD")
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest req) {
        String message = req != null ? req.getMessage() : "";
        String sessionId = req != null && req.getSessionId() != null ? req.getSessionId() : "session_default";

        String reply = aiAssistantService.generateResponse(message, sessionId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "reply", reply,
                "sessionId", sessionId,
                "source", "montclair_backend_ai"
        ));
    }

    /**
     * Retorna el contexto completo y en vivo del hotel en formato JSON estructurado.
     * Ideal para que n8n o agentes externos usen como Tool o System Context.
     */
    @Operation(summary = "Obtiene el contexto en vivo de la base de datos para agentes de IA (n8n, voz)")
    @GetMapping("/context")
    public ResponseEntity<Map<String, Object>> getHotelContext() {
        return ResponseEntity.ok(aiAssistantService.getHotelLiveContext());
    }

    /**
     * Retorna el contexto en formato texto legible para inyectar directamente en prompts de LLMs.
     */
    @Operation(summary = "Obtiene el contexto en texto plano formateado para prompts")
    @GetMapping("/prompt-context")
    public ResponseEntity<Map<String, String>> getPromptContext() {
        return ResponseEntity.ok(Map.of(
                "context", aiAssistantService.getFormattedHotelContext()
        ));
    }

    /**
     * Endpoint directo de habitaciones disponibles para consultas del agente de IA o n8n.
     */
    @Operation(summary = "Lista todas las habitaciones disponibles para reserva")
    @GetMapping("/rooms/available")
    public ResponseEntity<List<Room>> getAvailableRooms() {
        return ResponseEntity.ok(roomRepository.findByStatus(RoomStatus.AVAILABLE));
    }

    /**
     * Endpoint de servicios vigentes para consultas del agente de IA o n8n.
     */
    @Operation(summary = "Lista los servicios activos con tarifas para el asistente IA")
    @GetMapping("/services")
    public ResponseEntity<List<Service>> getActiveServices() {
        return ResponseEntity.ok(serviceRepository.findByHidden(false));
    }
}

