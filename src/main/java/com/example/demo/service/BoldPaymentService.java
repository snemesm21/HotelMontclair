package com.example.demo.service;

import com.example.demo.dto.BoldCheckoutDTO;
import com.example.demo.dto.ReservationDTO;
import com.example.demo.errors.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Servicio de integración con la pasarela de pagos Bold (Colombia).
 * Genera la firma de integridad SHA-256 en el servidor manteniendo un monto fijo simbólico de "1"
 * y liquida el saldo total de la reserva cuando Bold aprueba la transacción.
 */
@Service
public class BoldPaymentService {

    private final ReservationManagerService reservationService;
    private final String apiKey;
    private final String secretKey;
    private final String fixedAmount;
    private final String currency;
    private final String frontendUrl;

    @Autowired
    public BoldPaymentService(
            ReservationManagerService reservationService,
            @Value("${hotelmontclair.bold.api-key:${BOLD_API_KEY:}}") String apiKey,
            @Value("${hotelmontclair.bold.secret-key:${BOLD_SECRET_KEY:}}") String secretKey,
            @Value("${hotelmontclair.bold.fixed-amount:${BOLD_FIXED_AMOUNT:1}}") String fixedAmount,
            @Value("${hotelmontclair.bold.currency:${BOLD_CURRENCY:USD}}") String currency,
            @Value("${hotelmontclair.frontend-url:${FRONTEND_URL:http://localhost:4200}}") String frontendUrl) {
        this.reservationService = reservationService;
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.fixedAmount = fixedAmount;
        this.currency = currency;
        this.frontendUrl = frontendUrl;
    }

    /**
     * Construye los parámetros firmados para abrir el modal de BoldCheckout en Angular.
     * Por regla solicitada, el valor a cobrar en Bold siempre se fija en "1" (configurable en BOLD_FIXED_AMOUNT).
     */
    public BoldCheckoutDTO createCheckoutSession(Long reservationId, String returnPath) {
        if (reservationId == null) {
            throw new IllegalArgumentException("El ID de la reserva es obligatorio.");
        }

        ReservationDTO reservation = reservationService.findSummaryById(reservationId);
        if (reservation == null) {
            throw new NotFoundException(reservationId);
        }

        if ("CANCELLED".equalsIgnoreCase(reservation.getStatus())) {
            throw new IllegalStateException("No se puede iniciar un pago para una reserva cancelada.");
        }

        if (reservation.isPaid()) {
            throw new IllegalStateException("La cuenta de esta reserva ya se encuentra pagada en su totalidad.");
        }

        if (apiKey == null || apiKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "Las llaves de Bold (BOLD_API_KEY y BOLD_SECRET_KEY) no están configuradas en el servidor.");
        }

        String orderId = "MC-RES-" + reservationId + "-" + System.currentTimeMillis();
        String amount = (fixedAmount == null || fixedAmount.isBlank()) ? "1" : fixedAmount.trim();
        String curr = (currency == null || currency.isBlank()) ? "USD" : currency.trim().toUpperCase();

        // Fórmula oficial de Bold: SHA256(orderId + amount + currency + secretKey)
        String rawSignature = orderId + amount + curr + secretKey.trim();
        String integritySignature = sha256Hex(rawSignature);

        String baseUrl = (frontendUrl == null || frontendUrl.isBlank())
                ? "http://localhost:4200"
                : frontendUrl.replaceAll("/+$", "");

        String cleanPath = (returnPath != null && returnPath.startsWith("/"))
                ? returnPath
                : "/reservations/" + reservationId;

        String redirectionUrl = baseUrl + cleanPath;

        String fullName = ((reservation.getClientFirstName() != null ? reservation.getClientFirstName() : "")
                + " "
                + (reservation.getClientLastName() != null ? reservation.getClientLastName() : "")).trim();

        String description = "Pago de Reserva #" + reservationId + " - MontClair Grand Hotel";

        return BoldCheckoutDTO.builder()
                .reservationId(reservationId)
                .orderId(orderId)
                .apiKey(apiKey.trim())
                .amount(amount)
                .currency(curr)
                .integritySignature(integritySignature)
                .description(description)
                .redirectionUrl(redirectionUrl)
                .customerEmail(reservation.getClientEmail())
                .customerFullName(fullName.isEmpty() ? "Huésped MontClair" : fullName)
                .customerPhone(reservation.getClientPhone())
                .build();
    }

    /**
     * Confirma el pago retornado por Bold y liquida la cuenta completa de la reserva.
     */
    public ReservationDTO confirmPayment(Long reservationId, String orderId, String txStatus) {
        if (reservationId == null) {
            throw new IllegalArgumentException("El ID de la reserva es obligatorio.");
        }
        if (orderId == null || !orderId.startsWith("MC-RES-" + reservationId + "-")) {
            throw new IllegalArgumentException("El identificador de orden de Bold no corresponde a la reserva #" + reservationId + ".");
        }
        if (txStatus == null || !"approved".equalsIgnoreCase(txStatus.trim())) {
            throw new IllegalStateException("El pago en Bold no fue aprobado (estado: " + txStatus + ").");
        }

        ReservationDTO existing = reservationService.findSummaryById(reservationId);
        if (existing == null) {
            throw new NotFoundException(reservationId);
        }
        if (existing.isPaid()) {
            return existing;
        }

        return reservationService.payStayAccount(reservationId);
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible calcular la firma SHA-256 para Bold.", e);
        }
    }
}

