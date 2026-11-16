package com.example.demo.service;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.entities.Client;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación del servicio de correo mediante la API REST oficial de Resend
 * (POST https://api.resend.com/emails).
 * Opera en modo fail-safe: cualquier error de red o falta de API Key se registra
 * en los logs sin interrumpir el flujo principal del negocio.
 */
@Service
public class ResendEmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailServiceImpl.class);

    private final boolean mailEnabled;
    private final String apiKey;
    private final String fromAddress;
    private final String replyTo;
    private final String resendApiUrl;
    private final EmailTemplateBuilder templateBuilder;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public ResendEmailServiceImpl(
            @Value("${hotelmontclair.mail.enabled:true}") boolean mailEnabled,
            @Value("${hotelmontclair.mail.resend-api-key:${RESEND_API_KEY:}}") String apiKey,
            @Value("${hotelmontclair.mail.from:MontClair Grand Hotel <reservas@angaritateamo.com>}") String fromAddress,
            @Value("${hotelmontclair.mail.reply-to:concierge@angaritateamo.com}") String replyTo,
            @Value("${hotelmontclair.mail.api-url:https://api.resend.com/emails}") String resendApiUrl,
            EmailTemplateBuilder templateBuilder) {
        this(
                mailEnabled,
                apiKey,
                fromAddress,
                replyTo,
                resendApiUrl,
                templateBuilder,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    ResendEmailServiceImpl(
            boolean mailEnabled,
            String apiKey,
            String fromAddress,
            String replyTo,
            String resendApiUrl,
            EmailTemplateBuilder templateBuilder,
            HttpClient httpClient) {
        this.mailEnabled = mailEnabled;
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
        this.replyTo = replyTo;
        this.resendApiUrl = resendApiUrl;
        this.templateBuilder = templateBuilder;
        this.objectMapper = new ObjectMapper();
        this.httpClient = httpClient;
    }

    @Override
    public boolean sendHtmlEmail(String to, String subject, String htmlContent) {
        if (!mailEnabled) {
            log.debug("Envío de correos deshabilitado (hotelmontclair.mail.enabled=false). Omitiendo correo a '{}'.", to);
            return false;
        }
        if (to == null || to.isBlank()) {
            log.warn("No se pudo enviar el correo '{}': destinatario vacío.", subject);
            return false;
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("RESEND_API_KEY no está configurada. Omitiendo envío de '{}' a '{}'.", subject, to);
            return false;
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("from", fromAddress);
            payload.put("to", List.of(to.trim()));
            payload.put("subject", subject);
            payload.put("html", htmlContent);
            if (replyTo != null && !replyTo.isBlank()) {
                payload.put("reply_to", replyTo.trim());
            }

            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder(URI.create(resendApiUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String emailId = extractEmailId(response.body());
                log.info("Correo enviado con Resend exitosamente [id={}, to={}, subject='{}']", emailId, to, subject);
                return true;
            } else {
                log.error("Resend rechazó el envío del correo [status={}, to={}, response={}]",
                        response.statusCode(), to, response.body());
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Envío de correo interrumpido hacia '{}': {}", to, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error al enviar correo con Resend hacia '{}': {}", to, e.getMessage());
            return false;
        }
    }

    @Override
    public void sendWelcomeEmail(Client client) {
        if (client == null || client.getEmail() == null || client.getEmail().isBlank()) {
            return;
        }
        String subject = "Bienvenido/a a MontClair Grand Hotel";
        String html = templateBuilder.buildWelcomeEmail(client);
        sendHtmlEmail(client.getEmail(), subject, html);
    }

    @Override
    public void sendReservationCreatedEmail(ReservationDTO reservation) {
        if (reservation == null || reservation.getClientEmail() == null || reservation.getClientEmail().isBlank()) {
            return;
        }
        String subject = "Confirmación de Reserva #" + reservation.getId() + " - MontClair Grand Hotel";
        String html = templateBuilder.buildReservationCreatedEmail(reservation);
        sendHtmlEmail(reservation.getClientEmail(), subject, html);
    }

    @Override
    public void sendReservationUpdatedEmail(ReservationDTO reservation) {
        if (reservation == null || reservation.getClientEmail() == null || reservation.getClientEmail().isBlank()) {
            return;
        }
        String subject = "Reserva Actualizada #" + reservation.getId() + " - MontClair Grand Hotel";
        String html = templateBuilder.buildReservationUpdatedEmail(reservation);
        sendHtmlEmail(reservation.getClientEmail(), subject, html);
    }

    @Override
    public void sendReservationCancelledEmail(ReservationDTO reservation) {
        if (reservation == null || reservation.getClientEmail() == null || reservation.getClientEmail().isBlank()) {
            return;
        }
        String subject = "Cancelación de Reserva #" + reservation.getId() + " - MontClair Grand Hotel";
        String html = templateBuilder.buildReservationCancelledEmail(reservation);
        sendHtmlEmail(reservation.getClientEmail(), subject, html);
    }

    @Override
    public void sendPaymentReceiptEmail(ReservationDTO reservation) {
        if (reservation == null || reservation.getClientEmail() == null || reservation.getClientEmail().isBlank()) {
            return;
        }
        String subject = "Comprobante de Pago - Reserva #" + reservation.getId() + " - MontClair Grand Hotel";
        String html = templateBuilder.buildPaymentReceiptEmail(reservation);
        sendHtmlEmail(reservation.getClientEmail(), subject, html);
    }

    @Override
    public void sendStayCompletedEmail(ReservationDTO reservation) {
        if (reservation == null || reservation.getClientEmail() == null || reservation.getClientEmail().isBlank()) {
            return;
        }
        String subject = "Gracias por su estancia (#" + reservation.getId() + ") - MontClair Grand Hotel";
        String html = templateBuilder.buildStayCompletedEmail(reservation);
        sendHtmlEmail(reservation.getClientEmail(), subject, html);
    }

    private String extractEmailId(String body) {
        if (body == null || body.isBlank()) {
            return "unknown";
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            return node.path("id").asText("unknown");
        } catch (Exception ignored) {
            return "unknown";
        }
    }
}

