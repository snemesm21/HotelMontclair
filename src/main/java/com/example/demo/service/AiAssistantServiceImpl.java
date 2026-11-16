package com.example.demo.service;

import com.example.demo.entities.Room;
import com.example.demo.entities.RoomStatus;
import com.example.demo.entities.RoomType;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.RoomTypeRepository;
import com.example.demo.repository.ServiceRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Implementación del servicio de Asistente IA Concierge de MontClair Grand Hotel.
 * Consulta en tiempo real las entidades de base de datos (RoomType, Room, Service)
 * para responder consultas precisas y puede comunicarse con n8n enviándole el contexto vivo.
 */
@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Value("${ai.n8n.webhook-url:https://primary-production-4cb5.up.railway.app/webhook/chat-montclair}")
    private String n8nWebhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String generateResponse(String userMessage, String sessionId) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "¡Hola! Soy el Concierge Virtual de MontClair Grand Hotel. ¿En qué puedo asistirle hoy?";
        }

        String trimmed = userMessage.trim();

        // 1. Intentar delegar la consulta a n8n si el webhook está configurado, inyectando el contexto de la BD
        String n8nReply = tryInvokeN8n(trimmed, sessionId);
        if (n8nReply != null && !n8nReply.trim().isEmpty()) {
            return n8nReply;
        }

        // 2. Si n8n no está disponible o no responde, usar el motor de IA local alimentado por la BD real
        return generateDynamicResponseFromDatabase(trimmed);
    }

    @Override
    public Map<String, Object> getHotelLiveContext() {
        Map<String, Object> context = new LinkedHashMap<>();

        // 1. Tipos de habitación y tarifas
        List<RoomType> roomTypes = roomTypeRepository.findAll();
        List<Map<String, Object>> typesList = new ArrayList<>();
        for (RoomType type : roomTypes) {
            Map<String, Object> t = new HashMap<>();
            t.put("id", type.getId());
            t.put("name", type.getName());
            t.put("description", type.getDescription());
            t.put("pricePerNight", type.getPricePerNight());
            typesList.add(t);
        }
        context.put("roomTypes", typesList);

        // 2. Habitaciones disponibles por estado
        List<Room> availableRooms = roomRepository.findByStatus(RoomStatus.AVAILABLE);
        context.put("totalAvailableRooms", availableRooms.size());

        // 3. Servicios activos del hotel
        List<com.example.demo.entities.Service> services = serviceRepository.findByHidden(false);
        List<Map<String, Object>> servicesList = new ArrayList<>();
        for (com.example.demo.entities.Service s : services) {
            Map<String, Object> serv = new HashMap<>();
            serv.put("name", s.getName());
            serv.put("description", s.getDescription());
            serv.put("price", s.getPrice());
            serv.put("schedule", s.getSchedule());
            serv.put("tag", s.getTag());
            servicesList.add(serv);
        }
        context.put("services", servicesList);

        // 4. Políticas y contacto
        Map<String, String> policies = new HashMap<>();
        policies.put("checkInTime", "15:00");
        policies.put("checkOutTime", "12:00");
        policies.put("phone", "+377 93 12 34 56");
        policies.put("email", "reservas@montclairgrandhotel.com");
        policies.put("location", "Mónaco");
        context.put("hotelPolicies", policies);

        return context;
    }

    @Override
    public String getFormattedHotelContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== INFORMACIÓN EN VIVO DE MONTCLAIR GRAND HOTEL ===\n\n");

        sb.append("POLÍTICAS Y UBICACIÓN:\n");
        sb.append("- Ubicación: Mónaco\n");
        sb.append("- Horario de Check-in: 15:00 hrs\n");
        sb.append("- Horario de Check-out: 12:00 hrs\n");
        sb.append("- Teléfono de atención: +377 93 12 34 56\n");
        sb.append("- Correo electrónico: reservas@montclairgrandhotel.com\n\n");

        sb.append("HABITACIONES Y SUITES DISPONIBLES EN SISTEMA:\n");
        List<RoomType> types = roomTypeRepository.findAll();
        for (RoomType t : types) {
            sb.append(String.format("• %s: %s - Tarifa: $%.2f USD por noche\n",
                    t.getName(), t.getDescription(), t.getPricePerNight()));
        }
        sb.append("\n");

        sb.append("SERVICIOS EXCLUSIVOS ACTIVOS:\n");
        List<com.example.demo.entities.Service> services = serviceRepository.findByHidden(false);
        for (com.example.demo.entities.Service s : services) {
            sb.append(String.format("• %s (%s): %s | Precio: $%.2f | Horario: %s\n",
                    s.getName(), s.getTag(), s.getDescription(), s.getPrice(), s.getSchedule()));
        }

        return sb.toString();
    }

    /**
     * Intenta consultar el Webhook de n8n enviándole la consulta y el contexto de la BD
     */
    private String tryInvokeN8n(String userMessage, String sessionId) {
        if (n8nWebhookUrl == null || n8nWebhookUrl.trim().isEmpty() || n8nWebhookUrl.contains("localhost:5678")) {
            return null;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("message", userMessage);
            body.put("sessionId", sessionId);
            body.put("hotelContext", getHotelLiveContext());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(n8nWebhookUrl, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map respMap = response.getBody();
                Object reply = respMap.get("reply");
                if (reply == null) reply = respMap.get("output");
                if (reply == null) reply = respMap.get("response");
                if (reply != null) {
                    return reply.toString();
                }
            }
        } catch (Exception e) {
            // Si n8n no está en línea o falla, se continúa al motor local con datos de la BD
        }
        return null;
    }

    /**
     * Genera respuestas en lenguaje natural usando los datos exactos de la base de datos
     */
    private String generateDynamicResponseFromDatabase(String query) {
        String q = query.toLowerCase();

        // 1. Consulta sobre habitaciones o tarifas
        if (q.contains("habitacion") || q.contains("habitaciones") || q.contains("suite") ||
            q.contains("suites") || q.contains("tarifa") || q.contains("precio") || q.contains("costo")) {

            List<RoomType> types = roomTypeRepository.findAll();
            if (types.isEmpty()) {
                return "Actualmente estamos actualizando nuestro inventario de suites. Por favor comuníquese con recepción al +377 93 12 34 56.";
            }

            StringBuilder reply = new StringBuilder("En MontClair Grand Hotel disponemos de las siguientes suites y opciones de hospedaje registradas en nuestro sistema:\n\n");
            for (RoomType t : types) {
                reply.append(String.format("• **%s** — $%.2f por noche\n  *%s*\n\n",
                        t.getName(), t.getPricePerNight(), t.getDescription()));
            }
            reply.append("Puede seleccionar su habitación y fechas deseadas desde el botón dorado **Reservar Ahora** en la parte superior.");
            return reply.toString();
        }

        // 2. Consulta sobre servicios, spa, gastronomía o amenidades
        if (q.contains("servicio") || q.contains("spa") || q.contains("restaurante") ||
            q.contains("piscina") || q.contains("gimnasio") || q.contains("desayuno") ||
            q.contains("comida") || q.contains("masaje")) {

            List<com.example.demo.entities.Service> services = serviceRepository.findByHidden(false);
            if (services.isEmpty()) {
                return "MontClair Grand Hotel ofrece servicios premium de conserjería, spa y alta cocina las 24 horas.";
            }

            StringBuilder reply = new StringBuilder("Estos son los servicios exclusivos activos en nuestras instalaciones:\n\n");
            for (com.example.demo.entities.Service s : services) {
                reply.append(String.format("• **%s** ($%.2f)\n  *%s*\n  🕒 *Horario: %s*\n\n",
                        s.getName(), s.getPrice(), s.getDescription(), s.getSchedule()));
            }
            return reply.toString();
        }

        // 3. Horarios de check-in / check-out
        if (q.contains("check-in") || q.contains("check in") || q.contains("check-out") ||
            q.contains("check out") || q.contains("horario") || q.contains("llegada") || q.contains("salida")) {
            return "Nuestros horarios oficiales de estancia son:\n\n" +
                   "• **Check-In:** A partir de las **15:00 hrs**\n" +
                   "• **Check-Out:** Hasta las **12:00 hrs** del mediodía\n\n" +
                   "Si requiere ingresar antes (*Early Check-In*) o extender su estancia (*Late Check-Out*), indíquelo en sus notas de reserva o consulte en recepción.";
        }

        // 4. Contacto y ubicación
        if (q.contains("contacto") || q.contains("telefono") || q.contains("correo") ||
            q.contains("email") || q.contains("ubicacion") || q.contains("donde") || q.contains("direccion")) {
            return "Información de contacto de MontClair Grand Hotel:\n\n" +
                   "• **Ubicación:** Mónaco\n" +
                   "• **Teléfono de Recepción:** +377 93 12 34 56\n" +
                   "• **Correo Institucional:** reservas@montclairgrandhotel.com\n" +
                   "• **Atención:** 24 horas al día, 7 días a la semana.";
        }

        // 5. Cómo reservar
        if (q.contains("reserva") || q.contains("reservar") || q.contains("proceso") || q.contains("pasos")) {
            return "Para realizar su reserva de forma inmediata:\n\n" +
                   "1. Haga clic en **Reservar Ahora** en la barra superior.\n" +
                   "2. Elija sus fechas de entrada y salida, así como el número de personas.\n" +
                   "3. Seleccione su suite preferida.\n" +
                   "4. Confirme sus datos personales y asegure su estancia.\n\n" +
                   "¿Desea conocer la tarifa o detalles de alguna suite en específico?";
        }

        // Respuesta general con resumen en vivo
        List<RoomType> types = roomTypeRepository.findAll();
        String sampleTypes = types.stream().map(RoomType::getName).limit(3).reduce((a, b) -> a + ", " + b).orElse("nuestras suites");

        return "Con mucho gusto puedo asistirle con información de MontClair Grand Hotel. Actualmente contamos con opciones de hospedaje como " + sampleTypes + ", servicios de Spa y gastronomía de autor.\n\n" +
               "Puede consultarme sobre:\n" +
               "• Tarifas y tipos de habitaciones\n" +
               "• Servicios y horarios de spa/restaurante\n" +
               "• Políticas de Check-in y Check-out\n" +
               "• Ayuda con el proceso de reserva en línea";
    }
}