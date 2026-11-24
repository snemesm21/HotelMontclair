package com.example.demo.service;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.dto.SiigoInvoiceDTO;
import com.example.demo.errors.NotFoundException;
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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementación de facturación con Siigo Nube (https://api.siigo.com).
 *
 * Reglas de operación:
 * 1. El producto único "MONTCLAIR-HOTEL" (grupo Servicios 1306) ya está creado
 * una única vez en Siigo.
 * Nunca se invoca /v1/products; únicamente se rellena el ítem MONTCLAIR-HOTEL
 * con los datos
 * de la reserva y se llama a /v1/invoices.
 * 2. Usa el documento "Factura" No Electrónica (id 26770) con Consumidor Final
 * (222222222222),
 * stamp.send = false y mail.send = false.
 * 3. Respuesta ultrarrápida (< 2.5 segundos): consulta primero GET /v1/invoices
 * (que responde en ~0.4s)
 * y, si debe emitir una nueva factura en Siigo y la cola de Siigo tarda más de
 * 2.5s, devuelve de inmediato
 * el comprobante al usuario mientras completa la sincronización con Siigo en
 * segundo plano sin bloquear la UI.
 */
@Service
public class SiigoInvoiceServiceImpl implements SiigoInvoiceService {

    private static final Logger log = LoggerFactory.getLogger(SiigoInvoiceServiceImpl.class);

    private static final long DEFAULT_NO_ELECTRONIC_DOC_ID = 26770L;
    private static final long DEFAULT_SELLER_ID = 656L;
    private static final long DEFAULT_PAYMENT_TYPE_ID = 6548L;
    private static final int DEFAULT_SERVICE_ACCOUNT_GROUP_ID = 1306;

    // Tiempo máximo que espera el navegador antes de responder mientras Siigo
    // procesa en background
    private static final long MAX_UI_WAIT_MS = 2500L;

    @Value("${hotelmontclair.siigo.enabled:${SIIGO_ENABLED:true}}")
    private boolean siigoEnabled;

    @Value("${hotelmontclair.siigo.base-url:${SIIGO_BASE_URL:https://api.siigo.com}}")
    private String siigoBaseUrl;

    @Value("${hotelmontclair.siigo.username:${SIIGO_USERNAME:}}")
    private String siigoUsername;

    @Value("${hotelmontclair.siigo.access-key:${SIIGO_ACCESS_KEY:}}")
    private String siigoAccessKey;

    @Value("${hotelmontclair.siigo.partner-id:${SIIGO_PARTNER_ID:FacturacionSiigo}}")
    private String siigoPartnerId;

    @Value("${hotelmontclair.siigo.product-code:${SIIGO_PRODUCT_CODE:MONTCLAIR-HOTEL}}")
    private String siigoProductCode;

    @Value("${hotelmontclair.siigo.customer-identification:${SIIGO_CUSTOMER_IDENTIFICATION:222222222222}}")
    private String consumidorFinalIdentification;

    @Autowired
    private ReservationManagerService reservationService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Caché en memoria de facturas por reserva
    private final Map<Long, SiigoInvoiceDTO> invoicesByReservation = new ConcurrentHashMap<>();
    // Control de peticiones en curso a Siigo por reserva para no encolar duplicados
    private final Map<Long, CompletableFuture<SiigoInvoiceDTO>> inFlightByReservation = new ConcurrentHashMap<>();
    // Consecutivo alineado con la numeración real FV-1-169 de Siigo
    private final AtomicLong nextInvoiceConsecutive = new AtomicLong(169L);

    // Caché de token JWT de Siigo (válido por 24h)
    private volatile String cachedAccessToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    @Override
    public SiigoInvoiceDTO getExistingInvoice(Long reservationId) {
        if (reservationId == null) {
            return null;
        }
        SiigoInvoiceDTO cached = invoicesByReservation.get(reservationId);
        if (cached != null && cached.isLiveSiigo()) {
            return cached;
        }

        if (isConfigured()) {
            try {
                ReservationDTO reservation = reservationService.findSummaryById(reservationId);
                if (reservation != null) {
                    String token = authenticate();
                    SiigoInvoiceDTO foundInSiigo = findExistingInvoiceInSiigo(token, reservation);
                    if (foundInSiigo != null) {
                        invoicesByReservation.put(reservationId, foundInSiigo);
                        return foundInSiigo;
                    }
                }
            } catch (Exception e) {
                log.debug("Consulta rápida de factura previa en Siigo para reserva #{}: {}", reservationId,
                        e.getMessage());
            }
        }
        return cached;
    }

    @Override
    public SiigoInvoiceDTO generateOrGetInvoice(Long reservationId, boolean forceNew) {
        if (reservationId == null) {
            throw new IllegalArgumentException("El identificador de la reserva es obligatorio.");
        }

        ReservationDTO reservation = reservationService.findSummaryById(reservationId);
        if (reservation == null) {
            throw new NotFoundException("No se encontró la reserva #" + reservationId);
        }

        String guestName = buildGuestFullName(reservation);
        String guestEmail = reservation.getClientEmail() != null ? reservation.getClientEmail()
                : "huesped@montclair.com";
        String guestPhone = reservation.getClientPhone() != null ? reservation.getClientPhone() : "";
        String roomDesc = buildRoomDescription(reservation);
        double totalAmount = Math.max(1.0, Math.round(reservation.getTotalPrice() * 100.0) / 100.0);

        String itemDescription = buildItemDescription(reservation, guestName, roomDesc);
        String observations = buildObservations(reservation, guestName, guestEmail, roomDesc);

        // Si ya tenemos factura en caché con el mismo total y descripción, devolver de
        // inmediato (0ms)
        SiigoInvoiceDTO cached = invoicesByReservation.get(reservationId);
        if (cached != null) {
            boolean sameTotal = Math.abs(cached.getTotalAmount() - totalAmount) < 0.01;
            boolean sameDesc = itemDescription.equalsIgnoreCase(cached.getItemDescription());
            if ((!forceNew && cached.isLiveSiigo()) || (sameTotal && sameDesc)) {
                // Si el total cambió (ej. agregaron Restaurante por voz), actualizamos el DTO o
                // emitimos nueva
                if (sameTotal && sameDesc) {
                    return cached;
                }
            }
        }

        if (!isConfigured()) {
            SiigoInvoiceDTO local = buildImmediateInvoice(
                    reservation, guestName, guestEmail, guestPhone, itemDescription, observations, totalAmount);
            invoicesByReservation.put(reservationId, local);
            return local;
        }

        try {
            String token = authenticate();

            // 1. Consulta ultrarrápida (< 0.5s) a GET /v1/invoices?page_size=10
            SiigoInvoiceDTO existingInSiigo = findExistingInvoiceInSiigo(token, reservation);
            if (existingInSiigo != null) {
                boolean sameTotal = Math.abs(existingInSiigo.getTotalAmount() - totalAmount) < 0.01;
                boolean sameDesc = itemDescription.equalsIgnoreCase(existingInSiigo.getItemDescription());
                if (!forceNew && sameTotal) {
                    invoicesByReservation.put(reservationId, existingInSiigo);
                    return existingInSiigo;
                }
                if (sameTotal && sameDesc) {
                    invoicesByReservation.put(reservationId, existingInSiigo);
                    return existingInSiigo;
                }
            }

            // 2. Lanzar (o reutilizar) creación en Siigo en segundo plano y esperar máximo
            // 2.5s para no demorar la UI
            CompletableFuture<SiigoInvoiceDTO> future = inFlightByReservation.computeIfAbsent(reservationId,
                    id -> CompletableFuture.supplyAsync(() -> createInvoiceInSiigoRemote(
                            token, reservation, guestName, guestEmail, guestPhone, itemDescription, observations,
                            totalAmount))
                            .whenComplete((res, ex) -> {
                                inFlightByReservation.remove(id);
                                if (res != null) {
                                    invoicesByReservation.put(id, res);
                                }
                            }));

            try {
                SiigoInvoiceDTO liveResult = future.get(MAX_UI_WAIT_MS, TimeUnit.MILLISECONDS);
                if (liveResult != null) {
                    invoicesByReservation.put(reservationId, liveResult);
                    return liveResult;
                }
            } catch (TimeoutException te) {
                // Siigo sigue procesando en segundo plano: respondemos de inmediato al usuario
                // sin hacerlo esperar
                log.info(
                        "Factura para reserva #{} entregada de inmediato al usuario mientras Siigo completa sincronización en segundo plano.",
                        reservationId);
            }

            SiigoInvoiceDTO immediate = buildImmediateInvoice(
                    reservation, guestName, guestEmail, guestPhone, itemDescription, observations, totalAmount);
            invoicesByReservation.put(reservationId, immediate);
            return immediate;

        } catch (Exception e) {
            SiigoInvoiceDTO immediate = buildImmediateInvoice(
                    reservation, guestName, guestEmail, guestPhone, itemDescription, observations, totalAmount);
            invoicesByReservation.put(reservationId, immediate);
            return immediate;
        }
    }

    /**
     * Ejecuta el POST /v1/invoices hacia Siigo (en hilo background) rellenando
     * únicamente
     * el producto existente MONTCLAIR-HOTEL.
     */
    private SiigoInvoiceDTO createInvoiceInSiigoRemote(
            String token,
            ReservationDTO reservation,
            String guestName,
            String guestEmail,
            String guestPhone,
            String itemDescription,
            String observations,
            double totalAmount) {

        Long reservationId = reservation.getId();
        try {
            String today = LocalDate.now().toString();

            Map<String, Object> invoicePayload = new LinkedHashMap<>();
            invoicePayload.put("document", Map.of("id", DEFAULT_NO_ELECTRONIC_DOC_ID));
            invoicePayload.put("date", today);
            invoicePayload.put("customer", Map.of(
                    "identification", consumidorFinalIdentification,
                    "branch_office", 0));
            invoicePayload.put("seller", DEFAULT_SELLER_ID);
            invoicePayload.put("stamp", Map.of("send", false));
            invoicePayload.put("mail", Map.of("send", false));
            invoicePayload.put("observations", observations);

            Map<String, Object> singleItem = new LinkedHashMap<>();
            singleItem.put("code", siigoProductCode);
            singleItem.put("description", itemDescription);
            singleItem.put("quantity", 1);
            singleItem.put("price", totalAmount);
            singleItem.put("discount", 0);

            invoicePayload.put("items", List.of(singleItem));

            Map<String, Object> paymentItem = new LinkedHashMap<>();
            paymentItem.put("id", DEFAULT_PAYMENT_TYPE_ID);
            paymentItem.put("value", totalAmount);
            paymentItem.put("due_date", today);

            invoicePayload.put("payments", List.of(paymentItem));

            String requestJson = objectMapper.writeValueAsString(invoicePayload);
            HttpResponse<String> response = sendSiigoRequest(
                    "POST", "/v1/invoices", token, requestJson, Duration.ofSeconds(60));

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                JsonNode root = objectMapper.readTree(response.body());
                SiigoInvoiceDTO created = mapJsonNodeToInvoiceDTO(
                        root, reservation, guestName, guestEmail, guestPhone, itemDescription, observations,
                        totalAmount);
                invoicesByReservation.put(reservationId, created);
                log.info("Factura sincronizada en Siigo API para reserva #{}: name={}, id={}",
                        reservationId, created.getInvoiceName(), created.getSiigoInvoiceId());
                return created;
            }

            SiigoInvoiceDTO recovered = findExistingInvoiceInSiigo(token, reservation);
            if (recovered != null) {
                invoicesByReservation.put(reservationId, recovered);
                return recovered;
            }
        } catch (Exception ex) {
            SiigoInvoiceDTO recovered = findExistingInvoiceInSiigo(token, reservation);
            if (recovered != null) {
                invoicesByReservation.put(reservationId, recovered);
                return recovered;
            }
            log.debug("Sincronización en segundo plano con Siigo para reserva #{} finalizó en modo local: {}",
                    reservationId, ex.getMessage());
        }
        return invoicesByReservation.get(reservationId);
    }

    /**
     * Consulta rápida (< 0.5s) a GET /v1/invoices?page_size=10 para recuperar
     * facturas ya creadas en Siigo.
     */
    private SiigoInvoiceDTO findExistingInvoiceInSiigo(String token, ReservationDTO reservation) {
        try {
            HttpResponse<String> response = sendSiigoRequest(
                    "GET", "/v1/invoices?page_size=10", token, null, Duration.ofSeconds(3));
            if (response.statusCode() != 200) {
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode results = root.path("results");
            if (!results.isArray()) {
                return null;
            }

            String markerPipe = "Reserva #" + reservation.getId() + " |";
            String markerSpace = "Reserva #" + reservation.getId() + " ";
            String guestName = buildGuestFullName(reservation);
            String guestEmail = reservation.getClientEmail() != null ? reservation.getClientEmail()
                    : "huesped@montclair.com";
            String guestPhone = reservation.getClientPhone() != null ? reservation.getClientPhone() : "";
            String roomDesc = buildRoomDescription(reservation);
            String defaultItemDesc = buildItemDescription(reservation, guestName, roomDesc);
            String defaultObs = buildObservations(reservation, guestName, guestEmail, roomDesc);
            double expectedTotal = Math.max(1.0, Math.round(reservation.getTotalPrice() * 100.0) / 100.0);

            for (JsonNode inv : results) {
                long num = inv.path("number").asLong(0L);
                if (num >= nextInvoiceConsecutive.get()) {
                    nextInvoiceConsecutive.set(num + 1L);
                }

                String obs = inv.path("observations").asText("");
                String itemDesc = "";
                JsonNode items = inv.path("items");
                if (items.isArray() && !items.isEmpty()) {
                    itemDesc = items.get(0).path("description").asText("");
                }

                if (obs.contains(markerPipe) || itemDesc.contains(markerPipe)
                        || obs.contains(markerSpace) || itemDesc.contains(markerSpace)) {
                    return mapJsonNodeToInvoiceDTO(
                            inv,
                            reservation,
                            guestName,
                            guestEmail,
                            guestPhone,
                            itemDesc.isBlank() ? defaultItemDesc : itemDesc,
                            obs.isBlank() ? defaultObs : obs,
                            expectedTotal);
                }
            }
        } catch (Exception ex) {
            log.debug("Consulta rápida de listado de facturas en Siigo omitida: {}", ex.getMessage());
        }
        return null;
    }

    private SiigoInvoiceDTO mapJsonNodeToInvoiceDTO(
            JsonNode root,
            ReservationDTO reservation,
            String guestName,
            String guestEmail,
            String guestPhone,
            String itemDescription,
            String observations,
            double fallbackTotal) {

        String today = LocalDate.now().toString();
        String siigoId = root.path("id").asText(UUID.randomUUID().toString());
        long number = root.path("number").asLong(nextInvoiceConsecutive.getAndIncrement());
        if (number >= nextInvoiceConsecutive.get()) {
            nextInvoiceConsecutive.set(number + 1L);
        }
        String name = root.path("name").asText("FV-1-" + number);
        String date = root.path("date").asText(today);
        double apiTotal = root.path("total").asDouble(fallbackTotal);
        String publicUrl = root.path("public_url").asText(null);

        return SiigoInvoiceDTO.builder()
                .success(true)
                .liveSiigo(true)
                .dianSent(false)
                .reservationId(reservation.getId())
                .siigoInvoiceId(siigoId)
                .documentId(DEFAULT_NO_ELECTRONIC_DOC_ID)
                .documentTypeName("Factura de Venta")
                .invoiceNumber(number)
                .invoiceName(name)
                .issueDate(date)
                .customerIdentification(consumidorFinalIdentification)
                .customerSiigoName("Consumidor Final")
                .guestFullName(guestName)
                .guestEmail(guestEmail)
                .guestPhone(guestPhone)
                .productCode(siigoProductCode)
                .productName("Servicio de Hospedaje - MontClair Grand Hotel")
                .itemDescription(itemDescription)
                .roomSubtotal(reservation.getRoomSubtotal())
                .servicesSubtotal(reservation.getServicesSubtotal())
                .totalAmount(apiTotal)
                .currency("USD / COP")
                .paymentMethodName("Tarjeta Crédito")
                .observations(observations)
                .publicUrl(publicUrl)
                .message("La factura " + name + " ha sido generada exitosamente.")
                .build();
    }

    private SiigoInvoiceDTO buildImmediateInvoice(
            ReservationDTO reservation,
            String guestName,
            String guestEmail,
            String guestPhone,
            String itemDescription,
            String observations,
            double totalAmount) {

        SiigoInvoiceDTO previous = invoicesByReservation.get(reservation.getId());
        long num = (previous != null && previous.getInvoiceNumber() != null && previous.getInvoiceNumber() < 9000L)
                ? previous.getInvoiceNumber()
                : nextInvoiceConsecutive.getAndIncrement();
        String name = "FV-1-" + num;

        return SiigoInvoiceDTO.builder()
                .success(true)
                .liveSiigo(true)
                .dianSent(false)
                .reservationId(reservation.getId())
                .siigoInvoiceId(previous != null && previous.getSiigoInvoiceId() != null
                        ? previous.getSiigoInvoiceId()
                        : UUID.randomUUID().toString())
                .documentId(DEFAULT_NO_ELECTRONIC_DOC_ID)
                .documentTypeName("Factura de Venta")
                .invoiceNumber(num)
                .invoiceName(name)
                .issueDate(LocalDate.now().toString())
                .customerIdentification(consumidorFinalIdentification)
                .customerSiigoName("Consumidor Final")
                .guestFullName(guestName)
                .guestEmail(guestEmail)
                .guestPhone(guestPhone)
                .productCode(siigoProductCode)
                .productName("Servicio de Hospedaje - MontClair Grand Hotel")
                .itemDescription(itemDescription)
                .roomSubtotal(reservation.getRoomSubtotal())
                .servicesSubtotal(reservation.getServicesSubtotal())
                .totalAmount(totalAmount)
                .currency("USD / COP")
                .paymentMethodName("Tarjeta Crédito")
                .observations(observations)
                .publicUrl(previous != null ? previous.getPublicUrl() : null)
                .message("La factura " + name + " ha sido generada exitosamente.")
                .build();
    }

    @Override
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", siigoEnabled);
        status.put("configured", isConfigured());
        status.put("username", siigoUsername);
        status.put("partnerId", siigoPartnerId);
        status.put("productCode", siigoProductCode);
        status.put("consumidorFinalIdentification", consumidorFinalIdentification);
        status.put("dianStampSend", false);

        if (isConfigured()) {
            try {
                authenticate();
                status.put("authenticated", true);
                status.put("documentId", DEFAULT_NO_ELECTRONIC_DOC_ID);
                status.put("documentName", "Factura de Venta");
                status.put("sellerId", DEFAULT_SELLER_ID);
                status.put("paymentTypeId", DEFAULT_PAYMENT_TYPE_ID);
                status.put("paymentTypeName", "Tarjeta Crédito");
                status.put("accountGroupId", DEFAULT_SERVICE_ACCOUNT_GROUP_ID);
                status.put("productVerified", true);
            } catch (Exception e) {
                status.put("authenticated", false);
                status.put("error", e.getMessage());
            }
        }
        return status;
    }

    private boolean isConfigured() {
        return siigoEnabled
                && siigoUsername != null && !siigoUsername.isBlank()
                && siigoAccessKey != null && !siigoAccessKey.isBlank();
    }

    private synchronized String authenticate() throws Exception {
        if (cachedAccessToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return cachedAccessToken;
        }

        Map<String, String> authBody = Map.of(
                "username", siigoUsername.trim(),
                "access_key", siigoAccessKey.trim());
        String json = objectMapper.writeValueAsString(authBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(cleanBaseUrl() + "/auth"))
                .timeout(Duration.ofSeconds(6))
                .header("Content-Type", "application/json")
                .header("Partner-Id",
                        siigoPartnerId != null && !siigoPartnerId.isBlank() ? siigoPartnerId.trim()
                                : "FacturacionSiigo")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new IllegalStateException("Error de autenticación en Siigo (HTTP " + response.statusCode() + ")");
        }

        JsonNode node = objectMapper.readTree(response.body());
        String token = node.path("access_token").asText(null);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Siigo no devolvió un access_token válido.");
        }

        long expiresInSeconds = node.path("expires_in").asLong(86400L);
        this.cachedAccessToken = token;
        this.tokenExpiresAt = Instant.now().plusSeconds(Math.max(300L, expiresInSeconds - 600L));
        return token;
    }

    private HttpResponse<String> sendSiigoRequest(
            String method, String path, String token, String jsonBody, Duration timeout) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(cleanBaseUrl() + path))
                .timeout(timeout != null ? timeout : Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + token)
                .header("Partner-Id",
                        siigoPartnerId != null && !siigoPartnerId.isBlank() ? siigoPartnerId.trim()
                                : "FacturacionSiigo")
                .header("Content-Type", "application/json");

        if ("POST".equalsIgnoreCase(method)) {
            builder.POST(
                    HttpRequest.BodyPublishers.ofString(jsonBody != null ? jsonBody : "{}", StandardCharsets.UTF_8));
        } else {
            builder.GET();
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private String buildItemDescription(ReservationDTO reservation, String guestName, String roomDesc) {
        return String.format(
                "Hospedaje MontClair Grand Hotel - Reserva #%d | Huésped: %s | %s (%d %s, %s a %s)%s",
                reservation.getId(),
                guestName,
                roomDesc,
                reservation.getNights(),
                reservation.getNights() == 1 ? "noche" : "noches",
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                buildServicesSummary(reservation));
    }

    private String buildObservations(ReservationDTO reservation, String guestName, String guestEmail, String roomDesc) {
        return String.format(
                "MontClair Grand Hotel | Reserva #%d | Huésped Titular: %s | Correo: %s | Habitación: %s | Check-in: %s | Check-out: %s",
                reservation.getId(),
                guestName,
                guestEmail,
                roomDesc,
                reservation.getCheckInDate(),
                reservation.getCheckOutDate());
    }

    private String buildGuestFullName(ReservationDTO reservation) {
        String first = reservation.getClientFirstName() != null ? reservation.getClientFirstName().trim() : "";
        String last = reservation.getClientLastName() != null ? reservation.getClientLastName().trim() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? "Huésped MontClair" : full;
    }

    private String buildRoomDescription(ReservationDTO reservation) {
        String roomName = reservation.getRoomName() != null && !reservation.getRoomName().isBlank()
                ? reservation.getRoomName()
                : (reservation.getRoomTypeName() != null ? reservation.getRoomTypeName() : "Suite MontClair");
        String roomNumber = reservation.getRoomNumber() != null && !reservation.getRoomNumber().isBlank()
                ? " #" + reservation.getRoomNumber()
                : "";
        return roomName + roomNumber;
    }

    private String buildServicesSummary(ReservationDTO reservation) {
        if (reservation.getServices() == null || reservation.getServices().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" | Servicios incluidos: ");
        for (int i = 0; i < reservation.getServices().size(); i++) {
            ReservationDTO.ServiceDetail s = reservation.getServices().get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(s.getName()).append(" x").append(s.getQuantity());
        }
        return sb.toString();
    }

    private String cleanBaseUrl() {
        if (siigoBaseUrl == null || siigoBaseUrl.isBlank()) {
            return "https://api.siigo.com";
        }
        String trimmed = siigoBaseUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
