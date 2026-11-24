package com.example.demo.controller;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.dto.SiigoInvoiceDTO;
import com.example.demo.errors.NotFoundException;
import com.example.demo.service.JwtService;
import com.example.demo.service.ReservationManagerService;
import com.example.demo.service.SiigoInvoiceService;
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
 * Controlador REST para la generación y consulta de facturas simuladas en Siigo Nube (/api/invoices/siigo).
 * Utiliza el cliente genérico Consumidor Final (222222222222) y el producto único MONTCLAIR-HOTEL,
 * registrando el nombre completo del huésped en el detalle de la factura sin timbre DIAN.
 */
@RestController
@RequestMapping("/api/invoices/siigo")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class SiigoInvoiceController {

    @Autowired
    private SiigoInvoiceService siigoInvoiceService;

    @Autowired
    private ReservationManagerService reservationService;

    @Operation(summary = "Verifica el estado de conexión con Siigo API y los catálogos descubiertos")
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','CLIENT')")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(siigoInvoiceService.getStatus());
    }

    @Operation(summary = "Obtiene la factura simulada de Siigo ya generada para una reserva, si existe")
    @GetMapping("/reservation/{reservationId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','CLIENT')")
    public ResponseEntity<?> getExistingInvoice(@PathVariable Long reservationId) {
        ReservationDTO reservation = reservationService.findSummaryById(reservationId);
        if (reservation == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No se encontró la reserva #" + reservationId));
        }
        if (!canAccessReservation(reservation)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No tiene permisos para consultar la factura de esta reserva."));
        }

        SiigoInvoiceDTO existing = siigoInvoiceService.getExistingInvoice(reservationId);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Aún no se ha emitido factura en Siigo para la reserva #" + reservationId));
        }
        return ResponseEntity.ok(existing);
    }

    @Operation(summary = "Genera (o recupera) una factura simulada en Siigo Nube a nombre de Consumidor Final con el nombre del huésped y el producto único MONTCLAIR-HOTEL")
    @PostMapping("/reservation/{reservationId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','CLIENT')")
    public ResponseEntity<?> generateInvoice(
            @PathVariable Long reservationId,
            @RequestParam(defaultValue = "false") boolean forceNew) {
        try {
            ReservationDTO reservation = reservationService.findSummaryById(reservationId);
            if (reservation == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "No se encontró la reserva #" + reservationId));
            }
            if (!canAccessReservation(reservation)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "No tiene permisos para facturar esta reserva."));
            }

            SiigoInvoiceDTO invoice = siigoInvoiceService.generateOrGetInvoice(reservationId, forceNew);
            return ResponseEntity.ok(invoice);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al generar la factura en Siigo: " + e.getMessage()));
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
