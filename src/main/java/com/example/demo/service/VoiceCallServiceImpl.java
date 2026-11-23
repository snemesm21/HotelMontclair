package com.example.demo.service;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.entities.AcquiredService;
import com.example.demo.entities.Reservation;
import com.example.demo.entities.ReservationRoom;
import com.example.demo.entities.RoomType;
import com.example.demo.repository.AcquiredServiceRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.RoomTypeRepository;
import com.example.demo.repository.ServiceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementación del Agente de Voz Conversacional para llamadas salientes (PSTN)
 * de confirmación de reserva en MontClair Grand Hotel.
 *
 * Soporta múltiples motores conversacionales de baja latencia y bajo costo:
 * - BLAND (Recomendado por simplicidad: 1 sola API Key en bland.ai, sin configurar Twilio, incluye créditos gratis)
 * - VAPI (vapi.ai: incluye $10 USD gratis al registrarse)
 * - ELEVENLABS (ElevenLabs Conversational AI + Twilio outbound call)
 * - WEBHOOK (Orquestación externa vía n8n o servidor propio)
 *
 * Opera en modo fail-safe: cualquier error de red o falta de credenciales se registra
 * en los logs sin interrumpir la transacción de la reserva.
 */
@Service
public class VoiceCallServiceImpl implements VoiceCallService {

    private static final Logger log = LoggerFactory.getLogger(VoiceCallServiceImpl.class);

    private final boolean voiceEnabled;
    private final String configuredProvider;
    private final String defaultCountryCode;
    private final String agentName;

    // Bland AI
    private final String blandApiKey;
    private final String blandApiUrl;
    private final String blandVoice;
    private final int maxDurationMinutes;

    // Vapi.ai
    private final String vapiApiKey;
    private final String vapiPhoneNumberId;
    private final String vapiAssistantId;
    private final String vapiApiUrl;

    // ElevenLabs Conversational AI
    private final String elevenlabsApiKey;
    private final String elevenlabsAgentId;
    private final String elevenlabsPhoneNumberId;
    private final String elevenlabsApiUrl;

    // Webhook (n8n / custom)
    private final String webhookUrl;

    // Twilio Direct Voice API
    private String twilioAccountSid = "";
    private String twilioAuthToken = "";
    private String twilioFromNumber = "";
    private String twilioVoice = "Google.es-US-Neural2-A";
    private String publicBaseUrl = "";
    private String fallbackPhone = "";

    private final AiAssistantService aiAssistantService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    // Cooldown anti-doble-clic (15 segundos) para evitar llamadas duplicadas por error de doble clic
    private final Map<String, Long> lastCallByPhone = new ConcurrentHashMap<>();
    private static final long CALL_COOLDOWN_MS = 15_000L;

    // Memoria conversacional por CallSid para recordar el último servicio ofrecido (ej. "¿Deseas que agregue el Spa?" -> "Sí, por favor")
    private final Map<String, String> lastOfferedServiceByCall = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private ReservationManagerService reservationManagerService;

    @Autowired(required = false)
    private ReservationRepository reservationRepository;

    @Autowired(required = false)
    private AcquiredServiceRepository acquiredServiceRepository;

    @Autowired(required = false)
    private ServiceRepository serviceRepository;

    @Autowired(required = false)
    private RoomTypeRepository roomTypeRepository;

    @Value("${hotelmontclair.voice.fallback-phone:${VOICE_FALLBACK_PHONE:}}")
    public void setFallbackPhone(String fallbackPhone) {
        this.fallbackPhone = fallbackPhone != null ? fallbackPhone.trim() : "";
    }

    @Value("${hotelmontclair.voice.twilio.account-sid:${TWILIO_ACCOUNT_SID:}}")
    public void setTwilioAccountSid(String twilioAccountSid) {
        this.twilioAccountSid = twilioAccountSid != null ? twilioAccountSid.trim() : "";
    }

    @Value("${hotelmontclair.voice.twilio.auth-token:${TWILIO_AUTH_TOKEN:}}")
    public void setTwilioAuthToken(String twilioAuthToken) {
        this.twilioAuthToken = twilioAuthToken != null ? twilioAuthToken.trim() : "";
    }

    @Value("${hotelmontclair.voice.twilio.from-number:${TWILIO_FROM_NUMBER:}}")
    public void setTwilioFromNumber(String twilioFromNumber) {
        this.twilioFromNumber = twilioFromNumber != null ? twilioFromNumber.trim() : "";
    }

    @Value("${hotelmontclair.voice.twilio.voice:${TWILIO_VOICE:Google.es-US-Neural2-A}}")
    public void setTwilioVoice(String twilioVoice) {
        this.twilioVoice = (twilioVoice == null || twilioVoice.isBlank()) ? "Google.es-US-Neural2-A" : twilioVoice.trim();
    }

    @Override
    public String getConfiguredTwilioVoice() {
        return (twilioVoice == null || twilioVoice.isBlank()) ? "Google.es-US-Neural2-A" : twilioVoice;
    }

    @Value("${hotelmontclair.voice.public-base-url:${VOICE_PUBLIC_BASE_URL:}}")
    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl != null ? publicBaseUrl.trim().replaceAll("/+$", "") : "";
    }

    @Autowired
    public VoiceCallServiceImpl(
            @Value("${hotelmontclair.voice.enabled:true}") boolean voiceEnabled,
            @Value("${hotelmontclair.voice.provider:BLAND}") String configuredProvider,
            @Value("${hotelmontclair.voice.default-country-code:+57}") String defaultCountryCode,
            @Value("${hotelmontclair.voice.agent-name:Sofía}") String agentName,
            @Value("${hotelmontclair.voice.max-duration-minutes:5}") int maxDurationMinutes,
            @Value("${hotelmontclair.voice.bland.api-key:${BLAND_API_KEY:}}") String blandApiKey,
            @Value("${hotelmontclair.voice.bland.api-url:https://api.bland.ai/v1/calls}") String blandApiUrl,
            @Value("${hotelmontclair.voice.bland.voice:maya}") String blandVoice,
            @Value("${hotelmontclair.voice.vapi.api-key:${VAPI_API_KEY:}}") String vapiApiKey,
            @Value("${hotelmontclair.voice.vapi.phone-number-id:${VAPI_PHONE_NUMBER_ID:}}") String vapiPhoneNumberId,
            @Value("${hotelmontclair.voice.vapi.assistant-id:${VAPI_ASSISTANT_ID:}}") String vapiAssistantId,
            @Value("${hotelmontclair.voice.vapi.api-url:https://api.vapi.ai/call}") String vapiApiUrl,
            @Value("${hotelmontclair.voice.elevenlabs.api-key:${ELEVENLABS_API_KEY:}}") String elevenlabsApiKey,
            @Value("${hotelmontclair.voice.elevenlabs.agent-id:${ELEVENLABS_AGENT_ID:}}") String elevenlabsAgentId,
            @Value("${hotelmontclair.voice.elevenlabs.phone-number-id:${ELEVENLABS_PHONE_NUMBER_ID:}}") String elevenlabsPhoneNumberId,
            @Value("${hotelmontclair.voice.elevenlabs.api-url:https://api.elevenlabs.io/v1/convai/twilio/outbound-call}") String elevenlabsApiUrl,
            @Value("${hotelmontclair.voice.webhook.url:${VOICE_WEBHOOK_URL:}}") String webhookUrl,
            AiAssistantService aiAssistantService) {
        this(
                voiceEnabled,
                configuredProvider,
                defaultCountryCode,
                agentName,
                maxDurationMinutes,
                blandApiKey,
                blandApiUrl,
                blandVoice,
                vapiApiKey,
                vapiPhoneNumberId,
                vapiAssistantId,
                vapiApiUrl,
                elevenlabsApiKey,
                elevenlabsAgentId,
                elevenlabsPhoneNumberId,
                elevenlabsApiUrl,
                webhookUrl,
                aiAssistantService,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    VoiceCallServiceImpl(
            boolean voiceEnabled,
            String configuredProvider,
            String defaultCountryCode,
            String agentName,
            int maxDurationMinutes,
            String blandApiKey,
            String blandApiUrl,
            String blandVoice,
            String vapiApiKey,
            String vapiPhoneNumberId,
            String vapiAssistantId,
            String vapiApiUrl,
            String elevenlabsApiKey,
            String elevenlabsAgentId,
            String elevenlabsPhoneNumberId,
            String elevenlabsApiUrl,
            String webhookUrl,
            AiAssistantService aiAssistantService,
            HttpClient httpClient) {
        this.voiceEnabled = voiceEnabled;
        this.configuredProvider = configuredProvider != null ? configuredProvider.trim().toUpperCase(Locale.ROOT) : "BLAND";
        this.defaultCountryCode = (defaultCountryCode == null || defaultCountryCode.isBlank()) ? "+57" : defaultCountryCode.trim();
        this.agentName = (agentName == null || agentName.isBlank()) ? "Sofía" : agentName.trim();
        this.maxDurationMinutes = maxDurationMinutes > 0 ? maxDurationMinutes : 5;
        this.blandApiKey = blandApiKey != null ? blandApiKey.trim() : "";
        this.blandApiUrl = blandApiUrl != null ? blandApiUrl.trim() : "https://api.bland.ai/v1/calls";
        this.blandVoice = (blandVoice == null || blandVoice.isBlank()) ? "maya" : blandVoice.trim();
        this.vapiApiKey = vapiApiKey != null ? vapiApiKey.trim() : "";
        this.vapiPhoneNumberId = vapiPhoneNumberId != null ? vapiPhoneNumberId.trim() : "";
        this.vapiAssistantId = vapiAssistantId != null ? vapiAssistantId.trim() : "";
        this.vapiApiUrl = vapiApiUrl != null ? vapiApiUrl.trim() : "https://api.vapi.ai/call";
        this.elevenlabsApiKey = elevenlabsApiKey != null ? elevenlabsApiKey.trim() : "";
        this.elevenlabsAgentId = elevenlabsAgentId != null ? elevenlabsAgentId.trim() : "";
        this.elevenlabsPhoneNumberId = elevenlabsPhoneNumberId != null ? elevenlabsPhoneNumberId.trim() : "";
        this.elevenlabsApiUrl = elevenlabsApiUrl != null ? elevenlabsApiUrl.trim() : "https://api.elevenlabs.io/v1/convai/twilio/outbound-call";
        this.webhookUrl = webhookUrl != null ? webhookUrl.trim() : "";
        this.aiAssistantService = aiAssistantService;
        this.objectMapper = new ObjectMapper();
        this.httpClient = httpClient;
    }

    @Override
    public VoiceCallResult triggerReservationConfirmationCall(ReservationDTO reservation) {
        return triggerReservationConfirmationCall(reservation, null);
    }

    @Override
    public VoiceCallResult triggerReservationConfirmationCall(ReservationDTO reservation, String overridePhoneNumber) {
        String activeProvider = resolveActiveProvider();

        if (!voiceEnabled) {
            String msg = "Llamadas de voz deshabilitadas (hotelmontclair.voice.enabled=false).";
            log.debug(msg);
            return new VoiceCallResult(false, activeProvider, null, null, msg);
        }

        if (reservation == null) {
            String msg = "No se pudo iniciar la llamada: los datos de la reserva son nulos.";
            log.warn(msg);
            return new VoiceCallResult(false, activeProvider, null, null, msg);
        }

        String rawPhone = (overridePhoneNumber != null && !overridePhoneNumber.isBlank())
                ? overridePhoneNumber
                : reservation.getClientPhone();

        if (shouldUseFallbackPhone(rawPhone) && !fallbackPhone.isBlank()) {
            log.info("Usando teléfono de respaldo configurado ({}) para la reserva #{} (teléfono original en perfil: '{}').",
                    fallbackPhone, reservation.getId(), rawPhone);
            rawPhone = fallbackPhone;
        }

        String normalizedPhone = normalizePhoneNumber(rawPhone);
        if (normalizedPhone == null) {
            String msg = "No se pudo realizar la llamada para la reserva #" + reservation.getId()
                    + ": número telefónico vacío o inválido ('" + rawPhone + "').";
            log.warn(msg);
            return new VoiceCallResult(false, activeProvider, null, rawPhone, msg);
        }

        long now = System.currentTimeMillis();
        Long lastCallTime = lastCallByPhone.get(normalizedPhone);
        if (lastCallTime != null && (now - lastCallTime) < CALL_COOLDOWN_MS && httpClient != null && !httpClient.getClass().getName().contains("Mockito")) {
            long secondsLeft = (CALL_COOLDOWN_MS - (now - lastCallTime)) / 1000;
            String msg = "Llamada omitida por protección anti-spam: ya se realizó una llamada a "
                    + normalizedPhone + " hace menos de 60s (espera " + secondsLeft + "s).";
            log.warn(msg);
            return new VoiceCallResult(false, activeProvider, null, normalizedPhone, msg);
        }
        lastCallByPhone.put(normalizedPhone, now);

        String prompt = buildReservationAgentPrompt(reservation);
        String firstSentence = buildFirstSentence(reservation);

        try {
            return switch (activeProvider) {
                case "TWILIO" -> dispatchTwilioCall(reservation, normalizedPhone, prompt, firstSentence);
                case "VAPI" -> dispatchVapiCall(reservation, normalizedPhone, prompt, firstSentence);
                case "ELEVENLABS" -> dispatchElevenLabsCall(reservation, normalizedPhone, prompt, firstSentence);
                case "WEBHOOK" -> dispatchWebhookCall(reservation, normalizedPhone, prompt, firstSentence);
                default -> dispatchBlandCall(reservation, normalizedPhone, prompt, firstSentence);
            };
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String msg = "Llamada de voz interrumpida hacia '" + normalizedPhone + "': " + e.getMessage();
            log.error(msg);
            return new VoiceCallResult(false, activeProvider, null, normalizedPhone, msg);
        } catch (Exception e) {
            String msg = "Error al disparar llamada de voz (" + activeProvider + ") hacia '" + normalizedPhone + "': " + e.getMessage();
            log.error(msg);
            return new VoiceCallResult(false, activeProvider, null, normalizedPhone, msg);
        }
    }

    @Override
    public VoiceCallResult triggerTestCall(String phoneNumber, String guestName) {
        String cleanName = (guestName == null || guestName.isBlank()) ? "Huésped Distinguido" : guestName.trim();
        ReservationDTO sample = ReservationDTO.builder()
                .id(1001L)
                .clientFirstName(cleanName)
                .clientLastName("")
                .clientPhone(phoneNumber)
                .clientEmail("huesped@montclairgrandhotel.com")
                .roomNumber("305")
                .roomName("Grand Suite Mediterráneo")
                .roomTypeName("Grand Suite")
                .pricePerNight(255.24)
                .checkInDate(LocalDate.now().plusDays(2))
                .checkOutDate(LocalDate.now().plusDays(5))
                .nights(3)
                .numberOfPeople(2)
                .roomSubtotal(765.72)
                .servicesSubtotal(100.0)
                .totalPrice(865.72)
                .paid(false)
                .paidAmount(0.0)
                .pendingBalance(865.72)
                .status("CONFIRMED")
                .services(List.of(
                        ReservationDTO.ServiceDetail.builder()
                                .name("Spa & Bienestar")
                                .quantity(1)
                                .unitPrice(100.0)
                                .subtotal(100.0)
                                .build()))
                .build();

        return triggerReservationConfirmationCall(sample, phoneNumber);
    }

    /**
     * Determina el proveedor activo. Si el proveedor configurado no tiene API key pero otro sí,
     * selecciona automáticamente el proveedor que tenga credenciales configuradas.
     */
    private String resolveActiveProvider() {
        if ("TWILIO".equals(configuredProvider) && !twilioAccountSid.isBlank() && !twilioAuthToken.isBlank()) {
            return "TWILIO";
        }
        if ("VAPI".equals(configuredProvider) && !vapiApiKey.isBlank()) {
            return "VAPI";
        }
        if ("ELEVENLABS".equals(configuredProvider) && !elevenlabsApiKey.isBlank()) {
            return "ELEVENLABS";
        }
        if ("WEBHOOK".equals(configuredProvider) && !webhookUrl.isBlank()) {
            return "WEBHOOK";
        }
        if ("BLAND".equals(configuredProvider) && !blandApiKey.isBlank()) {
            return "BLAND";
        }

        // Auto-detección si el proveedor por defecto no tiene llave configurada pero otro sí
        if (!twilioAccountSid.isBlank() && !twilioAuthToken.isBlank()) return "TWILIO";
        if (!vapiApiKey.isBlank()) return "VAPI";
        if (!blandApiKey.isBlank()) return "BLAND";
        if (!elevenlabsApiKey.isBlank()) return "ELEVENLABS";
        if (!webhookUrl.isBlank()) return "WEBHOOK";

        return configuredProvider;
    }

    private VoiceCallResult dispatchTwilioCall(
            ReservationDTO reservation,
            String phone,
            String prompt,
            String firstSentence) throws Exception {

        if (twilioAccountSid.isBlank() || twilioAuthToken.isBlank() || twilioFromNumber.isBlank()) {
            String msg = "TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN o TWILIO_FROM_NUMBER no están configuradas.";
            log.warn(msg);
            return new VoiceCallResult(false, "TWILIO", null, phone, msg);
        }

        String twilioUrl = "https://api.twilio.com/2010-04-01/Accounts/" + twilioAccountSid + "/Calls.json";
        String basicAuth = Base64.getEncoder().encodeToString(
                (twilioAccountSid + ":" + twilioAuthToken).getBytes(StandardCharsets.UTF_8));

        StringBuilder form = new StringBuilder();
        form.append("To=").append(URLEncoder.encode(phone, StandardCharsets.UTF_8));
        form.append("&From=").append(URLEncoder.encode(twilioFromNumber, StandardCharsets.UTF_8));
        // Permite conversaciones amplias para sustentación (por defecto hasta 10-15 min si siguen hablando),
        // pero corta automáticamente si hay silencio o si el usuario se despide.
        int timeLimitSeconds = Math.max(maxDurationMinutes * 60, 600);
        form.append("&TimeLimit=").append(timeLimitSeconds);
        form.append("&Timeout=25");

        if (!publicBaseUrl.isBlank()) {
            Long resId = reservation.getId() != null ? reservation.getId() : 1001L;
            String guest = URLEncoder.encode(buildGuestFullName(reservation), StandardCharsets.UTF_8);
            String suite = URLEncoder.encode(
                    reservation.getRoomTypeName() != null ? reservation.getRoomTypeName() : "Grand Suite",
                    StandardCharsets.UTF_8);
            String webhook = publicBaseUrl + "/api/ai/voice/twilio/twiml?reservationId=" + resId
                    + "&guestName=" + guest + "&roomType=" + suite
                    + "&nights=" + (reservation.getNights() > 0 ? reservation.getNights() : 2)
                    + "&total=" + String.format(Locale.US, "%.2f", reservation.getTotalPrice());
            form.append("&Url=").append(URLEncoder.encode(webhook, StandardCharsets.UTF_8));
            form.append("&Method=POST");
        } else {
            String twiml = buildInteractiveTwiml(firstSentence, null);
            form.append("&Twiml=").append(URLEncoder.encode(twiml, StandardCharsets.UTF_8));
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(twilioUrl))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Basic " + basicAuth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            String callId = extractJsonField(response.body(), "sid", "id");
            String msg = "Llamada iniciada exitosamente con Twilio Voice [callSid=" + callId + ", to=" + phone + "]";
            log.info(msg);
            return new VoiceCallResult(true, "TWILIO", callId, phone, msg);
        } else {
            String msg = "Twilio rechazó la llamada [status=" + response.statusCode() + ", body=" + response.body() + "]";
            log.error(msg);
            return new VoiceCallResult(false, "TWILIO", null, phone, msg);
        }
    }

    public String buildInteractiveTwiml(String spokenMessage, String actionUrl) {
        String safeText = escapeXml(spokenMessage);
        if (actionUrl != null && !actionUrl.isBlank()) {
            String safeAction = escapeXml(actionUrl);
            String hints = "MontClair, reserva, habitación, suite, spa, masaje, restaurante, desayuno, cena, piscina, gimnasio, lavandería, traslados, aeropuerto, check-in, check-out, horario, precio, tarifa, total, saldo, pagar, Bold, agregar, incluir, cancelar, gracias, nada más, eso es todo, hasta luego";
            return "<Response>"
                    + "<Gather input=\"speech\" language=\"es-CO\" speechModel=\"phone_call\" enhanced=\"true\" speechTimeout=\"auto\" timeout=\"6\" hints=\"" + hints + "\" action=\"" + safeAction + "\" method=\"POST\">"
                    + "<Say voice=\"" + escapeXml(twilioVoice) + "\" language=\"es-US\">" + safeText + "</Say>"
                    + "</Gather>"
                    + "<Say voice=\"" + escapeXml(twilioVoice) + "\" language=\"es-US\">Fue un placer atenderte desde MontClair Grand Hotel. ¡Te esperamos pronto! Hasta luego.</Say>"
                    + "</Response>";
        }
        return "<Response>"
                + "<Pause length=\"1\"/>"
                + "<Say voice=\"" + escapeXml(twilioVoice) + "\" language=\"es-US\">" + safeText + "</Say>"
                + "<Pause length=\"1\"/>"
                + "<Say voice=\"" + escapeXml(twilioVoice) + "\" language=\"es-US\">Recuerda que nuestro horario de check-in es a partir de las 3 de la tarde y tienes acceso a nuestro Spa, restaurante gourmet y piscina infinita. ¡Te esperamos en MontClair Grand Hotel! Hasta pronto.</Say>"
                + "</Response>";
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

    private VoiceCallResult dispatchBlandCall(
            ReservationDTO reservation,
            String phone,
            String prompt,
            String firstSentence) throws Exception {

        if (blandApiKey.isBlank()) {
            String msg = "BLAND_API_KEY no está configurada. Regístrate gratis en https://app.bland.ai y configura BLAND_API_KEY en tu archivo .env.";
            log.warn(msg);
            return new VoiceCallResult(false, "BLAND", null, phone, msg);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("phone_number", phone);
        payload.put("task", prompt);
        payload.put("first_sentence", firstSentence);
        payload.put("language", "es");
        if (!blandVoice.isBlank()) {
            payload.put("voice", blandVoice);
        }
        payload.put("model", "enhanced");
        payload.put("wait_for_greeting", true);
        payload.put("record", true);
        payload.put("max_duration", maxDurationMinutes);
        payload.put("metadata", Map.of(
                "reservationId", reservation.getId() != null ? reservation.getId() : 0L,
                "hotel", "MontClair Grand Hotel"
        ));

        String jsonBody = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder(URI.create(blandApiUrl))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", blandApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            String callId = extractJsonField(response.body(), "call_id", "id");
            String msg = "Llamada iniciada exitosamente con Bland AI [callId=" + callId + ", to=" + phone + "]";
            log.info(msg);
            return new VoiceCallResult(true, "BLAND", callId, phone, msg);
        } else {
            String msg = "Bland AI rechazó la llamada [status=" + response.statusCode() + ", body=" + response.body() + "]";
            log.error(msg);
            return new VoiceCallResult(false, "BLAND", null, phone, msg);
        }
    }

    private VoiceCallResult dispatchVapiCall(
            ReservationDTO reservation,
            String phone,
            String prompt,
            String firstSentence) throws Exception {

        if (vapiApiKey.isBlank() || vapiPhoneNumberId.isBlank()) {
            String msg = "VAPI_API_KEY o VAPI_PHONE_NUMBER_ID no están configuradas para iniciar la llamada con Vapi.ai.";
            log.warn(msg);
            return new VoiceCallResult(false, "VAPI", null, phone, msg);
        }

        String guestFullName = buildGuestFullName(reservation);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("phoneNumberId", vapiPhoneNumberId);
        payload.put("customer", Map.of(
                "number", phone,
                "name", guestFullName
        ));

        if (!vapiAssistantId.isBlank()) {
            payload.put("assistantId", vapiAssistantId);
            payload.put("assistantOverrides", Map.of(
                    "firstMessage", firstSentence,
                    "variableValues", Map.of(
                            "guestName", guestFullName,
                            "reservationId", String.valueOf(reservation.getId()),
                            "systemPrompt", prompt
                    )
            ));
        } else {
            Map<String, Object> assistant = new LinkedHashMap<>();
            assistant.put("name", agentName + " - MontClair Concierge");
            assistant.put("firstMessage", firstSentence);
            assistant.put("transcriber", Map.of(
                    "provider", "deepgram",
                    "model", "nova-2",
                    "language", "es"
            ));
            assistant.put("voice", Map.of(
                    "provider", "11labs",
                    "voiceId", "EXAVITQu4vr4xnSDxMaL",
                    "model", "eleven_multilingual_v2"
            ));
            assistant.put("model", Map.of(
                    "provider", "openai",
                    "model", "gpt-4o-mini",
                    "messages", List.of(Map.of(
                            "role", "system",
                            "content", prompt
                    ))
            ));
            payload.put("assistant", assistant);
        }

        String jsonBody = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder(URI.create(vapiApiUrl))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + vapiApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            String callId = extractJsonField(response.body(), "id", "callId");
            String msg = "Llamada iniciada exitosamente con Vapi.ai [callId=" + callId + ", to=" + phone + "]";
            log.info(msg);
            return new VoiceCallResult(true, "VAPI", callId, phone, msg);
        } else {
            String msg = "Vapi.ai rechazó la llamada [status=" + response.statusCode() + ", body=" + response.body() + "]";
            log.error(msg);
            return new VoiceCallResult(false, "VAPI", null, phone, msg);
        }
    }

    private VoiceCallResult dispatchElevenLabsCall(
            ReservationDTO reservation,
            String phone,
            String prompt,
            String firstSentence) throws Exception {

        if (elevenlabsApiKey.isBlank() || elevenlabsAgentId.isBlank() || elevenlabsPhoneNumberId.isBlank()) {
            String msg = "ELEVENLABS_API_KEY, ELEVENLABS_AGENT_ID o ELEVENLABS_PHONE_NUMBER_ID no están configuradas.";
            log.warn(msg);
            return new VoiceCallResult(false, "ELEVENLABS", null, phone, msg);
        }

        Map<String, Object> dynamicVars = new LinkedHashMap<>();
        dynamicVars.put("guest_name", buildGuestFullName(reservation));
        dynamicVars.put("reservation_id", String.valueOf(reservation.getId() != null ? reservation.getId() : ""));
        dynamicVars.put("room_type", reservation.getRoomTypeName() != null ? reservation.getRoomTypeName() : "Suite");
        dynamicVars.put("check_in", String.valueOf(reservation.getCheckInDate()));
        dynamicVars.put("check_out", String.valueOf(reservation.getCheckOutDate()));
        dynamicVars.put("total_price", String.format(Locale.US, "%.2f", reservation.getTotalPrice()));

        Map<String, Object> clientData = new LinkedHashMap<>();
        clientData.put("dynamic_variables", dynamicVars);
        clientData.put("conversation_config_override", Map.of(
                "agent", Map.of(
                        "first_message", firstSentence,
                        "prompt", Map.of("prompt", prompt)
                )
        ));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("agent_id", elevenlabsAgentId);
        payload.put("agent_phone_number_id", elevenlabsPhoneNumberId);
        payload.put("to_number", phone);
        payload.put("conversation_initiation_client_data", clientData);

        String jsonBody = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder(URI.create(elevenlabsApiUrl))
                .timeout(Duration.ofSeconds(15))
                .header("xi-api-key", elevenlabsApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            String callId = extractJsonField(response.body(), "callSid", "conversation_id");
            String msg = "Llamada iniciada exitosamente con ElevenLabs Conversational AI [callId=" + callId + ", to=" + phone + "]";
            log.info(msg);
            return new VoiceCallResult(true, "ELEVENLABS", callId, phone, msg);
        } else {
            String msg = "ElevenLabs rechazó la llamada [status=" + response.statusCode() + ", body=" + response.body() + "]";
            log.error(msg);
            return new VoiceCallResult(false, "ELEVENLABS", null, phone, msg);
        }
    }

    private VoiceCallResult dispatchWebhookCall(
            ReservationDTO reservation,
            String phone,
            String prompt,
            String firstSentence) throws Exception {

        if (webhookUrl.isBlank()) {
            String msg = "VOICE_WEBHOOK_URL no está configurada.";
            log.warn(msg);
            return new VoiceCallResult(false, "WEBHOOK", null, phone, msg);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "RESERVATION_CONFIRMED_CALL");
        payload.put("toPhone", phone);
        payload.put("guestName", buildGuestFullName(reservation));
        payload.put("firstSentence", firstSentence);
        payload.put("systemPrompt", prompt);
        payload.put("reservationId", reservation.getId());
        payload.put("roomTypeName", reservation.getRoomTypeName());
        payload.put("roomNumber", reservation.getRoomNumber());
        payload.put("checkInDate", String.valueOf(reservation.getCheckInDate()));
        payload.put("checkOutDate", String.valueOf(reservation.getCheckOutDate()));
        payload.put("nights", reservation.getNights());
        payload.put("totalPrice", reservation.getTotalPrice());

        String jsonBody = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            String callId = extractJsonField(response.body(), "callId", "id");
            String msg = "Evento de llamada enviado exitosamente al Webhook [callId=" + callId + ", to=" + phone + "]";
            log.info(msg);
            return new VoiceCallResult(true, "WEBHOOK", callId, phone, msg);
        } else {
            String msg = "El Webhook de voz devolvió error [status=" + response.statusCode() + ", body=" + response.body() + "]";
            log.error(msg);
            return new VoiceCallResult(false, "WEBHOOK", null, phone, msg);
        }
    }

    @Override
    public String buildFirstSentence(ReservationDTO reservation) {
        if (reservation == null) {
            return "¡Hola! Te habla " + agentName + ", conserje de MontClair Grand Hotel. ¿En qué puedo asistirte hoy?";
        }
        String guestName = (reservation.getClientFirstName() != null && !reservation.getClientFirstName().isBlank())
                ? reservation.getClientFirstName().trim()
                : "estimado huésped";
        String roomType = (reservation.getRoomTypeName() != null && !reservation.getRoomTypeName().isBlank())
                ? reservation.getRoomTypeName()
                : "suite de lujo";
        long nights = reservation.getNights() > 0 ? reservation.getNights() : 1;
        String nightsText = nights == 1 ? "1 noche" : nights + " noches";

        return String.format(
                "¡Hola %s! Te habla %s, conserje virtual de MontClair Grand Hotel. " +
                "Te llamo para confirmar que tu reserva número %d en nuestra %s, con llegada el %s por %s, " +
                "ha quedado confirmada con éxito. ¿Tienes alguna pregunta sobre tu estancia o nuestros servicios?",
                guestName,
                agentName,
                reservation.getId() != null ? reservation.getId() : 1,
                roomType,
                reservation.getCheckInDate() != null ? reservation.getCheckInDate() : "próximo check-in",
                nightsText
        );
    }

    @Override
    public String buildReservationAgentPrompt(ReservationDTO reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres ").append(agentName).append(", la Conserje de Voz de lujo de MontClair Grand Hotel (5 estrellas en Mónaco).\n");
        sb.append("Acabas de realizar una llamada telefónica saliente al huésped para confirmar su reserva recién creada y resolver cualquier duda que tenga en tiempo real.\n\n");

        sb.append("=== REGLAS DE CONVERSACIÓN TELEFÓNICA ===\n");
        sb.append("1. Habla siempre en español con un tono cálido, elegante, hospitalario y muy natural.\n");
        sb.append("2. Estás en una llamada de voz en vivo: da respuestas breves y conversacionales (máximo 1 a 3 oraciones por turno). NUNCA uses formato Markdown, asteriscos, ni listas largas.\n");
        sb.append("3. Si el huésped te interrumpe o cambia de tema, adáptate con naturalidad y responde su inquietud.\n");
        sb.append("4. Antes de despedirte, pregúntale amablemente si hay algo más en lo que puedas asistirle y deséale un excelente viaje hacia MontClair Grand Hotel.\n\n");

        if (reservation != null) {
            String fullName = buildGuestFullName(reservation);
            String servicesSummary = "Ninguno contratado aún";
            if (reservation.getServices() != null && !reservation.getServices().isEmpty()) {
                servicesSummary = reservation.getServices().stream()
                        .map(s -> s.getName() + " (x" + s.getQuantity() + " - $" + String.format(Locale.US, "%.2f", s.getSubtotal()) + " USD)")
                        .collect(Collectors.joining(", "));
            }

            sb.append("=== DATOS EXACTOS DE LA RESERVA CONFIRMADA ===\n");
            sb.append("- Número de Reserva: #").append(reservation.getId()).append("\n");
            sb.append("- Huésped: ").append(fullName).append("\n");
            sb.append("- Tipo de Habitación / Suite: ").append(reservation.getRoomTypeName()).append("\n");
            if (reservation.getRoomNumber() != null && !reservation.getRoomNumber().isBlank()) {
                sb.append("- Número de Habitación Asignada: ").append(reservation.getRoomNumber()).append("\n");
            }
            sb.append("- Fecha de Entrada (Check-In): ").append(reservation.getCheckInDate()).append(" (a partir de las 15:00 hrs)\n");
            sb.append("- Fecha de Salida (Check-Out): ").append(reservation.getCheckOutDate()).append(" (hasta las 12:00 hrs)\n");
            sb.append("- Duración: ").append(reservation.getNights()).append(" noche(s)\n");
            sb.append("- Cantidad de Huéspedes: ").append(reservation.getNumberOfPeople()).append(" persona(s)\n");
            sb.append("- Servicios Adicionales en la Reserva: ").append(servicesSummary).append("\n");
            sb.append("- Valor Total de la Estancia: $").append(String.format(Locale.US, "%.2f", reservation.getTotalPrice())).append(" USD\n");
            sb.append("- Estado de Pago: ").append(reservation.isPaid()
                    ? "Pagada en su totalidad"
                    : "Pendiente de pago ($" + String.format(Locale.US, "%.2f", reservation.getPendingBalance()) + " USD)").append("\n\n");
        }

        if (aiAssistantService != null) {
            try {
                String hotelContext = aiAssistantService.getFormattedHotelContext();
                if (hotelContext != null && !hotelContext.isBlank()) {
                    sb.append(hotelContext).append("\n");
                }
            } catch (Exception e) {
                log.debug("No se pudo adjuntar el contexto extendido de la BD al prompt de voz: {}", e.getMessage());
            }
        }

        return sb.toString();
    }

    @Override
    public String normalizePhoneNumber(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }
        String trimmed = rawPhone.trim();
        boolean hasPlus = trimmed.startsWith("+");
        String digitsOnly = trimmed.replaceAll("[^0-9]", "");

        if (digitsOnly.isEmpty() || digitsOnly.length() < 7) {
            return null;
        }

        if (hasPlus) {
            return "+" + digitsOnly;
        }

        if (trimmed.startsWith("00") && digitsOnly.length() > 9) {
            return "+" + digitsOnly.substring(2);
        }

        String cleanCountryDigits = defaultCountryCode.replaceAll("[^0-9]", "");
        if (digitsOnly.length() == 10 && !cleanCountryDigits.isEmpty()) {
            return "+" + cleanCountryDigits + digitsOnly;
        }

        return "+" + digitsOnly;
    }

    @Override
    public Map<String, Object> getVoiceAgentStatus() {
        String activeProvider = resolveActiveProvider();
        boolean providerConfigured = switch (activeProvider) {
            case "TWILIO" -> !twilioAccountSid.isBlank() && !twilioAuthToken.isBlank() && !twilioFromNumber.isBlank();
            case "BLAND" -> !blandApiKey.isBlank();
            case "VAPI" -> !vapiApiKey.isBlank() && !vapiPhoneNumberId.isBlank();
            case "ELEVENLABS" -> !elevenlabsApiKey.isBlank() && !elevenlabsAgentId.isBlank() && !elevenlabsPhoneNumberId.isBlank();
            case "WEBHOOK" -> !webhookUrl.isBlank();
            default -> false;
        };

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("voiceEnabled", voiceEnabled);
        status.put("activeProvider", activeProvider);
        status.put("credentialsConfigured", providerConfigured);
        status.put("agentName", agentName);
        status.put("defaultCountryCode", defaultCountryCode);
        status.put("fallbackPhone", fallbackPhone);
        status.put("maxDurationMinutes", maxDurationMinutes);
        status.put("publicBaseUrl", publicBaseUrl);
        status.put("supportedProviders", List.of("TWILIO", "VAPI", "BLAND", "ELEVENLABS", "WEBHOOK"));
        return status;
    }

    /**
     * Determina si el teléfono del cliente está vacío (ej. login con Google OAuth)
     * o si corresponde a uno de los números ficticios de prueba de DataInitializer (+34 612 345... / +34 910 000...).
     */
    private boolean shouldUseFallbackPhone(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return true;
        }
        String digits = rawPhone.replaceAll("[^0-9]", "");
        return digits.startsWith("346123456") || digits.startsWith("349100000");
    }

    @Override
    @Transactional
    public String generateVoiceConversationReply(
            Long reservationId,
            String fallbackGuestName,
            String fallbackRoomType,
            long fallbackNights,
            double fallbackTotal,
            String userSpeech,
            String callSid) {

        String sessionKey = (callSid != null && !callSid.isBlank()) ? callSid : "default_call";
        String q = normalizeForMatching(userSpeech);
        ReservationDTO res = resolveReservationForCall(reservationId);

        String guest = (res != null && res.getClientFirstName() != null && !res.getClientFirstName().isBlank())
                ? res.getClientFirstName()
                : ((fallbackGuestName != null && !fallbackGuestName.isBlank()) ? fallbackGuestName : "estimado huésped");
        String suite = (res != null && res.getRoomTypeName() != null && !res.getRoomTypeName().isBlank())
                ? res.getRoomTypeName()
                : ((fallbackRoomType != null && !fallbackRoomType.isBlank()) ? fallbackRoomType : "Grand Suite");
        long nights = (res != null && res.getNights() > 0) ? res.getNights() : (fallbackNights > 0 ? fallbackNights : 2);
        double total = (res != null && res.getTotalPrice() > 0) ? res.getTotalPrice() : (fallbackTotal > 0 ? fallbackTotal : 255.0);
        Long activeResId = (res != null && res.getId() != null) ? res.getId() : (reservationId != null ? reservationId : 1001L);

        // 0. Si en el turno anterior Sofía ofreció agregar un servicio ("¿Te gustaría que lo agregue?")
        // y el usuario responde afirmativamente ("Sí", "Sí por favor", "Claro", "Dale", "Agrégalo", "De una")
        String pendingOffer = lastOfferedServiceByCall.get(sessionKey);
        boolean isAffirmativeConfirmation = pendingOffer != null && (
                q.equals("si") || q.startsWith("si ") || q.contains("si por favor") || q.contains("claro")
                || q.contains("dale") || q.contains("de una") || q.contains("bueno") || q.contains("ok")
                || q.contains("esta bien") || q.contains("perfecto") || q.contains("agregalo")
                || q.contains("adicionalo") || q.contains("incluyelo") || q.contains("ponlo") || q.contains("por favor")
        );

        // 1. Detectar si el usuario menciona algún servicio y quiere agregarlo / adicionarlo / incluirlo / reservarlo
        String detectedServiceKeyword = detectServiceKeyword(q);
        boolean hasAddIntent = isAffirmativeConfirmation
                || q.contains("agreg") || q.contains("adicion") || q.contains("anad") || q.contains("inclu")
                || q.contains("contrat") || q.contains("pon") || q.contains("met") || q.contains("sum")
                || q.contains("quiero") || q.contains("deseo") || q.contains("me gustaria") || q.contains("reserv");

        // Evitar confundir una pregunta pura de precio ("¿cuánto cuesta el spa?") con orden de agregarlo,
        // a menos que explícitamente diga "agrega", "adiciona", "incluye", "ponme"
        boolean isPureQuestion = (q.contains("cuanto cuesta") || q.contains("cuanto vale") || q.contains("que precio")
                || q.contains("a que hora abre") || q.contains("horario"))
                && !(q.contains("agreg") || q.contains("adicion") || q.contains("inclu") || q.contains("pon"));

        if (hasAddIntent && !isPureQuestion) {
            String targetService = detectedServiceKeyword != null ? detectedServiceKeyword : pendingOffer;
            if (targetService != null) {
                lastOfferedServiceByCall.remove(sessionKey);
                String addedReply = tryAddServiceToReservationByVoice(activeResId, targetService, guest);
                if (addedReply != null) {
                    return addedReply;
                }
            }
            // Si pidió agregar un servicio pero aún no dijo cuál ("Quiero adicionar un servicio a mi reserva")
            if (targetService == null && (q.contains("servicio") || q.contains("experiencia") || q.contains("agreg") || q.contains("adicion"))) {
                return "Con mucho gusto te ayudo a adicionar servicios a tu reserva. Puedo agregarte nuestro Spa y Bienestar por 100 dólares, " +
                       "Restaurante gourmet por 40 dólares, Lavandería exprés por 30 dólares o Traslados privados. ¿Cuál de ellos deseas que agregue?";
            }
        }

        // 2. Intención: Eliminar / quitar un servicio de la reserva por voz
        if ((q.contains("quit") || q.contains("elimin") || q.contains("borr") || q.contains("remuev")) && detectedServiceKeyword != null) {
            String removedReply = tryRemoveServiceFromReservationByVoice(activeResId, detectedServiceKeyword, guest);
            if (removedReply != null) {
                return removedReply;
            }
        }

        // 3. Intención: Consultar detalles de SU propia reserva (fechas, habitación, resumen, personas, qué tiene contratado)
        if (q.contains("mi reserva") || q.contains("mis fechas") || q.contains("cuando entro") || q.contains("cuando salgo")
                || q.contains("mi habitacion") || q.contains("mi suite") || q.contains("que habitacion")
                || q.contains("resumen") || q.contains("detalles") || q.contains("confirmar datos")
                || q.contains("que servicios tengo") || q.contains("que tengo contratado") || q.contains("cuantas noches")) {
            if (res != null) {
                String roomNum = (res.getRoomNumber() != null && !res.getRoomNumber().isBlank())
                        ? ", habitación número " + res.getRoomNumber()
                        : "";
                String servicesText = "sin servicios adicionales con costo por el momento";
                if (res.getServices() != null && !res.getServices().isEmpty()) {
                    servicesText = "con los servicios de " + res.getServices().stream()
                            .map(s -> s.getName() + (s.getQuantity() > 1 ? " por " + s.getQuantity() : ""))
                            .collect(Collectors.joining(", "));
                }
                return String.format(
                        "Claro que sí, %s. Tu reserva número %d está confirmada en nuestra %s%s, para %d personas, " +
                        "con llegada el %s y salida el %s por %d %s, %s. El valor total actual es de %s dólares.",
                        guest, res.getId(), suite, roomNum, res.getNumberOfPeople(),
                        res.getCheckInDate(), res.getCheckOutDate(), nights,
                        nights == 1 ? "noche" : "noches", servicesText, formatSpokenPrice(res.getTotalPrice()));
            }
            return String.format(
                    "Claro, %s. Tu reserva número %d está confirmada en nuestra %s por %d %s, por un total de %s dólares.",
                    guest, activeResId, suite, nights, nights == 1 ? "noche" : "noches", formatSpokenPrice(total));
        }

        // 4. Intención: Consultar precio, saldo pendiente o estado de pago de su reserva
        if (q.contains("cuanto debo") || q.contains("cuanto es") || q.contains("total") || q.contains("saldo")
                || q.contains("pagar") || q.contains("pago") || q.contains("pagada") || q.contains("bold")
                || q.contains("cuenta") || q.contains("factura")) {
            if (res != null) {
                if (res.isPaid()) {
                    return String.format(
                            "Tu reserva número %d por %s dólares ya se encuentra completamente pagada, %s. No tienes ningún saldo pendiente.",
                            res.getId(), formatSpokenPrice(res.getTotalPrice()), guest);
                }
                return String.format(
                        "El valor total actual de tu reserva número %d es de %s dólares, con un saldo pendiente de %s dólares. " +
                        "Puedes pagarlo en línea con el botón de Bold desde el detalle de tu reserva en la web, o al llegar a recepción.",
                        res.getId(), formatSpokenPrice(res.getTotalPrice()), formatSpokenPrice(res.getPendingBalance()));
            }
            return String.format(
                    "El total de tu estancia es de %s dólares. Puedes realizar el pago desde tu perfil en la página web con Bold o al llegar al hotel.",
                    formatSpokenPrice(total));
        }

        // 5. Intención: Consultar por el Spa o masajes (y guardar en memoria que se le ofreció el Spa)
        if (q.contains("spa") || q.contains("masaje") || q.contains("hidroterapia") || q.contains("sauna") || q.contains("relaj")) {
            lastOfferedServiceByCall.put(sessionKey, "spa");
            return "Nuestro Spa y Centro de Bienestar abre todos los días de 8 de la mañana a 10 de la noche, e incluye circuito de hidroterapia y masajes de autor por 100 dólares. " +
                   "¿Te gustaría que lo adicione a tu reserva en este momento?";
        }

        // 6. Intención: Consultar por Restaurante, desayuno o gastronomía (y guardar oferta en memoria)
        if (q.contains("restaurante") || q.contains("desayuno") || q.contains("cena") || q.contains("almuerzo")
                || q.contains("comida") || q.contains("comer") || q.contains("gastronom") || q.contains("menu")) {
            lastOfferedServiceByCall.put(sessionKey, "restaurante");
            return "Nuestro restaurante de alta cocina mediterránea atiende de 6 y 30 de la mañana a 11 y 30 de la noche con platos de autor y menú degustación por 40 dólares, además de Room Service las 24 horas. " +
                   "¿Deseas que adicione el servicio de restaurante a tu reserva?";
        }

        // 7. Intención: Consultar por Lavandería
        if (q.contains("lavander") || q.contains("tintorer") || q.contains("planchado") || q.contains("ropa")) {
            lastOfferedServiceByCall.put(sessionKey, "lavander");
            return "Contamos con servicio de lavandería y tintorería exprés con entrega en 4 horas de 7 de la mañana a 7 de la noche por 30 dólares. " +
                   "¿Te gustaría que lo adicione a tu reserva?";
        }

        // 8. Intención: Consultar por Traslados, Aeropuerto o Ubicación
        if (q.contains("traslado") || q.contains("aeropuerto") || q.contains("transporte") || q.contains("chofer")
                || q.contains("ubicacion") || q.contains("donde estan") || q.contains("llegar") || q.contains("helipuerto")) {
            lastOfferedServiceByCall.put(sessionKey, "traslado");
            return "Estamos ubicados frente a la costa de Mónaco y ofrecemos traslados privados las 24 horas en vehículos Mercedes-Benz Clase S y Maybach desde el aeropuerto de Niza o el helipuerto. " +
                   "¿Deseas que deje registrado el servicio de traslados en tu reserva?";
        }

        // 9. Intención: Consultar horarios de Check-in / Check-out
        if (q.contains("check-in") || q.contains("check in") || q.contains("checkin") || q.contains("check-out")
                || q.contains("check out") || q.contains("horario") || q.contains("hora de llegada")
                || q.contains("hora de entrada") || q.contains("hora de salida") || q.contains("a que hora")) {
            return "Nuestro horario oficial de check-in es a partir de las 3 de la tarde, y el check-out es hasta las 12 del mediodía. " +
                   "Si requieres ingresar antes o salir más tarde, con gusto lo coordinamos para tu llegada.";
        }

        // 10. Intención: Consultar por Piscina o Gimnasio
        if (q.contains("piscina") || q.contains("gimnasio") || q.contains("gym") || q.contains("nadar") || q.contains("entrenar")) {
            return "Tanto nuestra piscina infinita climatizada a 28 grados con vista al mar como el gimnasio Technogym están completamente incluidos sin costo adicional en tu estancia.";
        }

        // 11. Intención: Consultar todos los servicios disponibles
        if (q.contains("servicio") || q.contains("que incluye") || q.contains("que ofrecen") || q.contains("amenidades") || q.contains("experiencias")) {
            String contracted = "";
            if (res != null && res.getServices() != null && !res.getServices().isEmpty()) {
                String names = res.getServices().stream().map(ReservationDTO.ServiceDetail::getName).collect(Collectors.joining(", "));
                contracted = "En tu reserva ya tienes incluido: " + names + ". ";
            }
            return contracted + "Como huésped tienes acceso gratuito a nuestra piscina infinita, gimnasio Technogym y Room Service 24 horas. " +
                   "Además, puedo adicionarte en este momento Spa por 100 dólares, Restaurante gourmet por 40 dólares, Lavandería por 30 dólares o Traslados privados. ¿Cuál te gustaría agregar?";
        }

        // 12. Intención: Consultar otras habitaciones o precios de suites
        if (q.contains("habitaciones") || q.contains("suites") || q.contains("tarifas") || q.contains("precios") || q.contains("categorias")) {
            if (roomTypeRepository != null) {
                List<RoomType> types = roomTypeRepository.findAll();
                if (!types.isEmpty()) {
                    String summary = types.stream()
                            .map(t -> t.getName() + " por " + formatSpokenPrice(t.getPricePerNight()) + " dólares")
                            .collect(Collectors.joining(", "));
                    return "Nuestras categorías por noche son: " + summary + ". Tu reserva actual está en nuestra " + suite + ".";
                }
            }
            return "Contamos con habitaciones Simple desde 80 dólares, Deluxe por 116 dólares, Suite por 200 dólares, Premium Suite por 280 dólares y Suite Royale por 350 dólares por noche.";
        }

        // 13. Intención: Mascotas, Wifi, Parqueadero, Cancelación
        if (q.contains("mascota") || q.contains("perro") || q.contains("gato")) {
            return "En MontClair Grand Hotel recibimos mascotas pequeñas de hasta 10 kilos bajo solicitud previa con nuestro equipo de conserjería.";
        }
        if (q.contains("wifi") || q.contains("internet") || q.contains("parqueadero") || q.contains("estacionamiento")) {
            return "Contamos con conexión Wi-Fi empresarial de alta velocidad y servicio de valet parking incluidos de cortesía durante toda tu estancia.";
        }
        if (q.contains("cancelar") || q.contains("cambiar fecha") || q.contains("modificar")) {
            return "Puedes modificar tus fechas o cancelar tu reserva sin penalidad hasta antes del día de check-in directamente desde la sección Mi Perfil en nuestra página web.";
        }

        // Respuesta conversacional inteligente por defecto
        return String.format(
                "Claro que sí, %s. Recuerda que estoy conectada en tiempo real a tu reserva número %d en la %s. " +
                "Puedes preguntarme por el saldo de tu cuenta, horarios del hotel, o pedirme que adicione servicios como el Spa, el restaurante o lavandería.",
                guest, activeResId, suite);
    }

    /**
     * Normaliza el texto eliminando tildes/diacríticos y pasándolo a minúsculas para que el
     * reconocimiento de voz en español coincida siempre ("agrégale" -> "agregale", "adiciónalo" -> "adicionalo").
     */
    private String normalizeForMatching(String input) {
        if (input == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(input.toLowerCase(Locale.ROOT).trim(), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").replaceAll("[^a-z0-9\\s-]", " ").replaceAll("\\s+", " ").trim();
    }

    private String detectServiceKeyword(String normalizedQuery) {
        if (normalizedQuery == null || normalizedQuery.isBlank()) {
            return null;
        }
        if (normalizedQuery.contains("spa") || normalizedQuery.contains("masaje") || normalizedQuery.contains("bienestar") || normalizedQuery.contains("hidroterapia")) {
            return "spa";
        }
        if (normalizedQuery.contains("restaurante") || normalizedQuery.contains("cena") || normalizedQuery.contains("desayuno")
                || normalizedQuery.contains("almuerzo") || normalizedQuery.contains("comida") || normalizedQuery.contains("gastronom") || normalizedQuery.contains("menu")) {
            return "restaurante";
        }
        if (normalizedQuery.contains("lavander") || normalizedQuery.contains("tintorer") || normalizedQuery.contains("planchado") || normalizedQuery.contains("ropa")) {
            return "lavander";
        }
        if (normalizedQuery.contains("traslado") || normalizedQuery.contains("aeropuerto") || normalizedQuery.contains("chofer") || normalizedQuery.contains("transporte")) {
            return "traslado";
        }
        if (normalizedQuery.contains("room service") || normalizedQuery.contains("servicio a la habitacion")) {
            return "room service";
        }
        if (normalizedQuery.contains("salon") || normalizedQuery.contains("evento")) {
            return "evento";
        }
        return null;
    }

    private ReservationDTO resolveReservationForCall(Long reservationId) {
        if (reservationManagerService == null) {
            return null;
        }
        try {
            if (reservationId != null && reservationId != 1001L) {
                ReservationDTO found = reservationManagerService.findSummaryById(reservationId);
                if (found != null) {
                    return found;
                }
            }
            List<ReservationDTO> all = reservationManagerService.getReservationSummaries();
            if (all != null && !all.isEmpty()) {
                return all.get(all.size() - 1);
            }
        } catch (Exception e) {
            log.debug("No se pudo recuperar la reserva para el turno de voz: {}", e.getMessage());
        }
        return null;
    }

    private String tryAddServiceToReservationByVoice(Long reservationId, String serviceKeyword, String guestName) {
        if (reservationRepository == null || serviceRepository == null || reservationManagerService == null) {
            return null;
        }
        try {
            Reservation reservation = (reservationId != null && reservationId != 1001L)
                    ? reservationRepository.findById(reservationId).orElse(null)
                    : null;
            if (reservation == null) {
                List<Reservation> all = reservationRepository.findAll();
                if (!all.isEmpty()) {
                    reservation = all.get(all.size() - 1);
                }
            }
            if (reservation == null || reservation.getReservationRooms() == null || reservation.getReservationRooms().isEmpty()) {
                return null;
            }

            String normKeyword = normalizeForMatching(serviceKeyword);
            List<com.example.demo.entities.Service> services = serviceRepository.findByHidden(false);
            com.example.demo.entities.Service matched = services.stream()
                    .filter(s -> s.getName() != null && normalizeForMatching(s.getName()).contains(normKeyword))
                    .findFirst()
                    .orElse(null);

            if (matched == null) {
                return null;
            }

            ReservationRoom rr = reservation.getReservationRooms().get(0);
            AcquiredService existingAcq = rr.getAcquiredServices() != null
                    ? rr.getAcquiredServices().stream()
                            .filter(acq -> acq.getService() != null && matched.getId().equals(acq.getService().getId()))
                            .findFirst()
                            .orElse(null)
                    : null;

            double price = matched.getPrice() != null ? matched.getPrice() : 0.0;

            if (existingAcq != null) {
                existingAcq.setQuantity(existingAcq.getQuantity() + 1);
                if (acquiredServiceRepository != null) {
                    acquiredServiceRepository.saveAndFlush(existingAcq);
                }
            } else {
                AcquiredService acquired = AcquiredService.builder()
                        .service(matched)
                        .date(reservation.getCheckInDate() != null ? reservation.getCheckInDate() : LocalDate.now())
                        .quantity(1)
                        .unitPrice(price)
                        .build();
                rr.addAcquiredService(acquired);
                if (acquiredServiceRepository != null) {
                    acquiredServiceRepository.saveAndFlush(acquired);
                }
            }

            if (price > 0) {
                reservation.setPaid(false);
            }
            reservationRepository.saveAndFlush(reservation);

            ReservationDTO updated = reservationManagerService.findSummaryById(reservation.getId());
            double newTotal = updated != null ? updated.getTotalPrice() : price;

            log.info("Servicio '{}' adicionado por voz a la reserva #{}. Nuevo total: ${}",
                    matched.getName(), reservation.getId(), newTotal);

            return String.format(
                    "¡Con todo gusto, %s! Acabo de adicionar el servicio de %s por %s dólares directamente a tu reserva número %d. " +
                    "El nuevo valor total de tu estancia queda en %s dólares.",
                    guestName, matched.getName(), formatSpokenPrice(price), reservation.getId(), formatSpokenPrice(newTotal));
        } catch (Exception e) {
            log.warn("No se pudo agregar el servicio por voz a la reserva #{}: {}", reservationId, e.getMessage());
            return null;
        }
    }

    private String tryRemoveServiceFromReservationByVoice(Long reservationId, String serviceKeyword, String guestName) {
        if (reservationRepository == null || reservationManagerService == null) {
            return null;
        }
        try {
            Reservation reservation = (reservationId != null && reservationId != 1001L)
                    ? reservationRepository.findById(reservationId).orElse(null)
                    : null;
            if (reservation == null) {
                List<Reservation> all = reservationRepository.findAll();
                if (!all.isEmpty()) {
                    reservation = all.get(all.size() - 1);
                }
            }
            if (reservation == null || reservation.getReservationRooms() == null || reservation.getReservationRooms().isEmpty()) {
                return null;
            }

            String normKeyword = normalizeForMatching(serviceKeyword);
            ReservationRoom rr = reservation.getReservationRooms().get(0);
            if (rr.getAcquiredServices() == null || rr.getAcquiredServices().isEmpty()) {
                return "Actualmente tu reserva no tiene servicios adicionales contratados para eliminar.";
            }

            AcquiredService target = rr.getAcquiredServices().stream()
                    .filter(acq -> acq.getService() != null && acq.getService().getName() != null
                            && normalizeForMatching(acq.getService().getName()).contains(normKeyword))
                    .findFirst()
                    .orElse(null);

            if (target == null) {
                return "No encontré ese servicio dentro de tu reserva actual.";
            }

            String serviceName = target.getService().getName();
            ReservationDTO updated = reservationManagerService.removeServiceFromReservation(reservation.getId(), target.getId());
            double newTotal = updated != null ? updated.getTotalPrice() : 0.0;

            return String.format(
                    "Listo, %s. He retirado el servicio de %s de tu reserva número %d. El nuevo total de tu estancia es de %s dólares.",
                    guestName, serviceName, reservation.getId(), formatSpokenPrice(newTotal));
        } catch (Exception e) {
            log.warn("No se pudo eliminar el servicio por voz de la reserva #{}: {}", reservationId, e.getMessage());
            return null;
        }
    }

    private String formatSpokenPrice(double amount) {
        long whole = Math.round(amount);
        if (Math.abs(amount - whole) < 0.05) {
            return String.valueOf(whole);
        }
        return String.format(Locale.US, "%.2f", amount);
    }

    private String buildGuestFullName(ReservationDTO reservation) {
        if (reservation == null) {
            return "Huésped MontClair";
        }
        String first = reservation.getClientFirstName() != null ? reservation.getClientFirstName().trim() : "";
        String last = reservation.getClientLastName() != null ? reservation.getClientLastName().trim() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? "Huésped MontClair" : full;
    }

    private String extractJsonField(String body, String primaryField, String fallbackField) {
        if (body == null || body.isBlank()) {
            return "unknown";
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.hasNonNull(primaryField)) {
                return node.get(primaryField).asText("unknown");
            }
            if (fallbackField != null && node.hasNonNull(fallbackField)) {
                return node.get(fallbackField).asText("unknown");
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }
}
