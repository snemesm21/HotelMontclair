package com.example.demo.controller;

import com.example.demo.dto.BoldCheckoutDTO;
import com.example.demo.dto.ReservationDTO;
import com.example.demo.errors.NotFoundException;
import com.example.demo.service.BoldPaymentService;
import com.example.demo.service.JwtService;
import com.example.demo.service.ReservationManagerService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para la pasarela de pagos Bold (/api/payments/bold).
 * Genera firmas de integridad SHA-256 con monto fijo de "1" y confirma pagos aprobados.
 */
@RestController
@RequestMapping("/api/payments/bold")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class BoldPaymentController {

    @Autowired
    private BoldPaymentService boldPaymentService;

    @Autowired
    private ReservationManagerService reservationService;

    @Operation(summary = "Genera los parámetros firmados con SHA-256 para abrir el Botón de Pagos Bold (valor fijo = 1)")
    @PostMapping("/checkout/{reservationId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','CLIENT')")
    public ResponseEntity<?> createCheckout(
            @PathVariable Long reservationId,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            ReservationDTO reservation = reservationService.findSummaryById(reservationId);
            if (reservation == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "No se encontró la reserva #" + reservationId));
            }
            if (!canAccessReservation(reservation)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "No tiene permisos para pagar esta reserva."));
            }

            String returnPath = body != null ? body.get("returnPath") : null;
            BoldCheckoutDTO checkout = boldPaymentService.createCheckoutSession(reservationId, returnPath);
            return ResponseEntity.ok(checkout);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al inicializar pago con Bold: " + e.getMessage()));
        }
    }

    @Operation(summary = "Confirma el pago aprobado por Bold y liquida la cuenta de la reserva")
    @PostMapping("/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','CLIENT')")
    public ResponseEntity<?> confirmPayment(@RequestBody Map<String, Object> body) {
        try {
            if (body == null || !body.containsKey("reservationId")) {
                return ResponseEntity.badRequest().body(Map.of("message", "El reservationId es obligatorio."));
            }
            Long reservationId = Long.valueOf(String.valueOf(body.get("reservationId")));
            String orderId = body.get("orderId") != null ? String.valueOf(body.get("orderId")) : null;
            String txStatus = body.get("txStatus") != null ? String.valueOf(body.get("txStatus")) : null;

            ReservationDTO reservation = reservationService.findSummaryById(reservationId);
            if (reservation == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "No se encontró la reserva #" + reservationId));
            }
            if (!canAccessReservation(reservation)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "No tiene permisos para confirmar el pago de esta reserva."));
            }

            ReservationDTO updated = boldPaymentService.confirmPayment(reservationId, orderId, txStatus);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al confirmar pago de Bold: " + e.getMessage()));
        }
    }

    private boolean canAccessReservation(ReservationDTO reservation) {
        if (reservation == null) {
            return false;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtService.JwtClaims claims)) {
            return true;
        }
        if ("ADMIN".equalsIgnoreCase(claims.role())
                || "OPERATOR".equalsIgnoreCase(claims.role())
                || "OPERARIO".equalsIgnoreCase(claims.role())) {
            return true;
        }
        return reservation.getClientId() != null && claims.clientId().equals(reservation.getClientId());
    }
}

