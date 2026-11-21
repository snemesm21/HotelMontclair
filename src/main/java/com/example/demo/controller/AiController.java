package com.example.demo.controller;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.entities.Room;
import com.example.demo.entities.RoomStatus;
import com.example.demo.entities.Service;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.ServiceRepository;
import com.example.demo.service.AiAssistantService;
import com.example.demo.service.ReservationManagerService;
import com.example.demo.service.VoiceCallService;
import com.example.demo.service.VoiceCallService.VoiceCallResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para el Asistente IA de MontClair Grand Hotel (/api/ai).
 * Conecta el chat frontend y los agentes externos (n8n, telefonía, agente de voz conversacional)
 * directamente con los datos reales de la base de datos (habitaciones disponibles, servicios, tarifas y políticas).
 */
@Tag(name = "AI Concierge", description = "Endpoints para el agente de IA, asistente de voz conversacional y contexto en vivo de la BD")
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

    @Autowired(required = false)
    private VoiceCallService voiceCallService;

    @Autowired(required = false)
    private ReservationManagerService reservationService;

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

    /**
     * Consulta el estado de configuración del Agente de Voz Conversacional para llamadas salientes.
     */
    @Operation(summary = "Obtiene el estado de configuración del agente de voz conversacional")
    @GetMapping("/voice/status")
    public ResponseEntity<Map<String, Object>> getVoiceAgentStatus() {
        return ResponseEntity.ok(voiceCallService.getVoiceAgentStatus());
    }

    /**
     * Dispara manualmente una llamada del agente de voz conversacional para confirmar una reserva existente.
     * Permite enviar opcionalmente {"phoneNumber": "+57300..."} en el body para probar con otro número.
     */
    @Operation(summary = "Inicia una llamada telefónica del agente de voz para una reserva confirmada")
    @PostMapping("/voice/call/reservation/{reservationId}")
    public ResponseEntity<Map<String, Object>> callForReservation(
            @PathVariable Long reservationId,
            @RequestBody(required = false) Map<String, String> body) {

        ReservationDTO reservation = reservationService != null ? reservationService.findSummaryById(reservationId) : null;
        if (reservation == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "success", false,
                            "message", "No se encontró la reserva #" + reservationId
                    ));
        }

        String overridePhone = body != null ? body.get("phoneNumber") : null;
        VoiceCallResult result = voiceCallService.triggerReservationConfirmationCall(reservation, overridePhone);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", result.success());
        response.put("provider", result.provider());
        response.put("callId", result.callId());
        response.put("toPhone", result.toPhone());
        response.put("reservationId", reservationId);
        response.put("message", result.message());
        response.put("firstSentencePreview", voiceCallService.buildFirstSentence(reservation));

        return ResponseEntity.status(result.success() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Dispara una llamada de prueba inmediata del agente de voz a cualquier número telefónico.
     * Body esperado: {"phoneNumber": "+573001234567", "guestName": "Juan"}
     */
    @Operation(summary = "Realiza una llamada telefónica de prueba con el agente de voz conversacional")
    @PostMapping("/voice/call/test")
    public ResponseEntity<Map<String, Object>> testVoiceCall(@RequestBody Map<String, String> body) {
        String phone = body != null ? body.get("phoneNumber") : null;
        String guestName = body != null ? body.get("guestName") : "Juan";

        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "El campo 'phoneNumber' es obligatorio (ej. '+573001234567')."
            ));
        }

        VoiceCallResult result = voiceCallService.triggerTestCall(phone, guestName);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", result.success());
        response.put("provider", result.provider());
        response.put("callId", result.callId());
        response.put("toPhone", result.toPhone());
        response.put("message", result.message());

        return ResponseEntity.status(result.success() ? HttpStatus.OK : HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Endpoint TwiML inicial para llamadas salientes de Twilio.
     * Saluda al huésped con voz neural en español y abre el micrófono (<Gather input="speech">)
     * para sostener una conversación interactiva sobre la reserva y el hotel.
     */
    @Operation(summary = "Webhook TwiML inicial para llamadas conversacionales de Twilio")
    @RequestMapping(
            value = "/voice/twilio/twiml",
            method = {RequestMethod.GET, RequestMethod.POST},
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> twilioInitialTwiml(
            @RequestParam(required = false) Long reservationId,
            @RequestParam(required = false, defaultValue = "Huésped") String guestName,
            @RequestParam(required = false, defaultValue = "Grand Suite") String roomType,
            @RequestParam(required = false, defaultValue = "2") long nights,
            @RequestParam(required = false, defaultValue = "255.0") double total) {

        ReservationDTO reservation = (reservationId != null && reservationService != null)
                ? reservationService.findSummaryById(reservationId)
                : null;

        String greeting = reservation != null
                ? voiceCallService.buildFirstSentence(reservation)
                : "¡Hola " + guestName + "! Te habla Sofía, conserje virtual de MontClair Grand Hotel. "
                  + "Te llamo para confirmar con alegría tu reserva en nuestra " + roomType + " por " + nights + " noches. "
                  + "¿Tienes alguna pregunta sobre tu estancia, horarios de check-in o deseas agregar algún servicio como nuestro Spa?";

        String respondUrl = buildTwilioRespondUrl(reservationId, guestName, roomType, nights, total);
        String hints = "MontClair, reserva, habitación, suite, spa, masaje, restaurante, desayuno, cena, piscina, gimnasio, lavandería, traslados, aeropuerto, check-in, check-out, horario, precio, tarifa, total, saldo, pagar, Bold, agregar, incluir, cancelar, gracias, nada más, eso es todo, hasta luego";

        String xml = "<Response>"
                + "<Gather input=\"speech\" language=\"es-CO\" speechModel=\"phone_call\" enhanced=\"true\" speechTimeout=\"auto\" timeout=\"6\" hints=\"" + hints + "\" action=\"" + escapeXml(respondUrl) + "\" method=\"POST\">"
                + "<Say voice=\"Polly.Lupe-Neural\" language=\"es-US\">" + escapeXml(greeting) + "</Say>"
                + "</Gather>"
                + "<Say voice=\"Polly.Lupe-Neural\" language=\"es-US\">Fue un placer saludarte desde MontClair Grand Hotel. ¡Te esperamos pronto! Hasta luego.</Say>"
                + "</Response>";

        return ResponseEntity.ok(xml);
    }

    /**
     * Webhook TwiML que procesa cada turno de voz del huésped (SpeechResult de Twilio),
     * consulta el motor de voz de ultra-baja latencia conectado en vivo a la BD
     * (permitiendo incluso agregar servicios a la reserva por voz) y responde con voz neural.
     */
    @Operation(summary = "Webhook TwiML de respuesta conversacional en tiempo real para Twilio")
    @PostMapping(
            value = "/voice/twilio/respond",
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> twilioConversationRespond(
            @RequestParam(required = false) Long reservationId,
            @RequestParam(required = false, defaultValue = "Huésped") String guestName,
            @RequestParam(required = false, defaultValue = "Grand Suite") String roomType,
            @RequestParam(required = false, defaultValue = "2") long nights,
            @RequestParam(required = false, defaultValue = "255.0") double total,
            @RequestParam(value = "SpeechResult", required = false) String speechResult,
            @RequestParam(value = "CallSid", required = false, defaultValue = "twilio_call") String callSid) {

        if (speechResult == null || speechResult.isBlank()) {
            String xml = "<Response>"
                    + "<Say voice=\"Polly.Lupe-Neural\" language=\"es-US\">No alcancé a escucharte bien. Si necesitas algo más, recuerda que nuestra recepción está disponible las 24 horas. ¡Feliz día!</Say>"
                    + "</Response>";
            return ResponseEntity.ok(xml);
        }

        String lower = speechResult.toLowerCase();
        if (lower.contains("nada más") || lower.contains("nada mas") || lower.contains("eso es todo")
                || lower.contains("hasta luego") || lower.contains("adiós") || lower.contains("adios")
                || lower.contains("no gracias") || lower.contains("no, gracias") || lower.contains("chao")
                || lower.equals("no") || lower.equals("todo claro") || lower.contains("muchas gracias")) {
            String farewell = "<Response>"
                    + "<Say voice=\"Polly.Lupe-Neural\" language=\"es-US\">¡Perfecto, " + escapeXml(guestName) + "! Ha sido un gusto atenderte desde la conserjería de MontClair Grand Hotel. ¡Te deseamos un excelente viaje y te esperamos con los brazos abiertos! Hasta pronto.</Say>"
                    + "<Hangup/>"
                    + "</Response>";
            return ResponseEntity.ok(farewell);
        }

        String spokenReply = voiceCallService.generateVoiceConversationReply(
                reservationId, guestName, roomType, nights, total, speechResult);
        String cleanSpokenReply = cleanForSpeech(spokenReply);

        String respondUrl = buildTwilioRespondUrl(reservationId, guestName, roomType, nights, total);
        String hints = "MontClair, reserva, habitación, suite, spa, masaje, restaurante, desayuno, cena, piscina, gimnasio, lavandería, traslados, aeropuerto, check-in, check-out, horario, precio, tarifa, total, saldo, pagar, Bold, agregar, incluir, cancelar, gracias, nada más, eso es todo, hasta luego";

        String xml = "<Response>"
                + "<Gather input=\"speech\" language=\"es-CO\" speechModel=\"phone_call\" enhanced=\"true\" speechTimeout=\"auto\" timeout=\"6\" hints=\"" + hints + "\" action=\"" + escapeXml(respondUrl) + "\" method=\"POST\">"
                + "<Say voice=\"Polly.Lupe-Neural\" language=\"es-US\">" + escapeXml(cleanSpokenReply) + " ¿Hay algo más en lo que pueda ayudarte?</Say>"
                + "</Gather>"
                + "<Say voice=\"Polly.Lupe-Neural\" language=\"es-US\">Gracias por comunicarte con MontClair Grand Hotel. ¡Te esperamos pronto!</Say>"
                + "</Response>";

        return ResponseEntity.ok(xml);
    }

    private String buildTwilioRespondUrl(Long reservationId, String guestName, String roomType, long nights, double total) {
        String encGuest = java.net.URLEncoder.encode(guestName != null ? guestName : "Huésped", java.nio.charset.StandardCharsets.UTF_8);
        String encRoom = java.net.URLEncoder.encode(roomType != null ? roomType : "Grand Suite", java.nio.charset.StandardCharsets.UTF_8);
        return "/api/ai/voice/twilio/respond?reservationId=" + (reservationId != null ? reservationId : 1001L)
                + "&guestName=" + encGuest
                + "&roomType=" + encRoom
                + "&nights=" + nights
                + "&total=" + total;
    }

    private String cleanForSpeech(String text) {
        if (text == null) return "";
        return text
                .replaceAll("\\*+", "")
                .replaceAll("[•🕒⚜#]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

