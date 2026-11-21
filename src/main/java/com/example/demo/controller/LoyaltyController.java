package com.example.demo.controller;

import com.example.demo.dto.LoyaltySummaryDTO;
import com.example.demo.dto.PointAdjustmentRequestDTO;
import com.example.demo.dto.PointTransactionDTO;
import com.example.demo.dto.RedeemedRewardDTO;
import com.example.demo.entities.Client;
import com.example.demo.entities.PointTransactionType;
import com.example.demo.service.ClientService;
import com.example.demo.service.JwtService;
import com.example.demo.service.LoyaltyService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST del programa de fidelidad (/api/loyalty).
 * Expone resumen de puntos y categoría, historial de transacciones, sección de recompensas
 * canjeadas, canje de recompensas y ajustes manuales por administrador.
 */
@RestController
@RequestMapping("/api/loyalty")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;
    private final ClientService clientService;

    public LoyaltyController(LoyaltyService loyaltyService, ClientService clientService) {
        this.loyaltyService = loyaltyService;
        this.clientService = clientService;
    }

    // ==================== ENDPOINTS DEL USUARIO AUTENTICADO (/me) ====================

    @Operation(summary = "Obtiene el resumen de puntos, categoría y progreso del huésped autenticado")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LoyaltySummaryDTO> getMyLoyaltySummary() {
        Long clientId = resolveAuthenticatedClientId();
        return ResponseEntity.ok(loyaltyService.getClientLoyaltySummary(clientId));
    }

    @Operation(summary = "Obtiene el historial de transacciones de puntos del huésped autenticado")
    @GetMapping("/me/transactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PointTransactionDTO>> getMyTransactions(
            @RequestParam(required = false) String type) {
        Long clientId = resolveAuthenticatedClientId();
        PointTransactionType filterType = parseTransactionType(type);
        return ResponseEntity.ok(loyaltyService.getTransactionsByClient(clientId, filterType));
    }

    @Operation(summary = "Obtiene las recompensas canjeadas por el huésped autenticado (sección Canjeados)")
    @GetMapping("/me/redeemed")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RedeemedRewardDTO>> getMyRedeemedRewards() {
        Long clientId = resolveAuthenticatedClientId();
        return ResponseEntity.ok(loyaltyService.getRedeemedRewardsByClient(clientId));
    }

    @Operation(summary = "Canjea una recompensa del catálogo restando los puntos del huésped autenticado")
    @PostMapping("/me/redeem/{rewardId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RedeemedRewardDTO> redeemReward(@PathVariable Long rewardId) {
        Long clientId = resolveAuthenticatedClientId();
        RedeemedRewardDTO redeemed = loyaltyService.redeemReward(clientId, rewardId);
        return ResponseEntity.status(HttpStatus.CREATED).body(redeemed);
    }

    // ==================== ENDPOINTS POR CLIENTE / PERSONAL ====================

    @Operation(summary = "Obtiene el resumen de fidelidad de un cliente específico (propio o personal)")
    @GetMapping("/clients/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','CLIENT')")
    public ResponseEntity<LoyaltySummaryDTO> getClientLoyaltySummary(@PathVariable Long clientId) {
        verifyClientAccess(clientId);
        return ResponseEntity.ok(loyaltyService.getClientLoyaltySummary(clientId));
    }

    @Operation(summary = "Obtiene el historial de transacciones de puntos de un cliente (propio o personal)")
    @GetMapping("/clients/{clientId}/transactions")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','CLIENT')")
    public ResponseEntity<List<PointTransactionDTO>> getClientTransactions(
            @PathVariable Long clientId,
            @RequestParam(required = false) String type) {
        verifyClientAccess(clientId);
        PointTransactionType filterType = parseTransactionType(type);
        return ResponseEntity.ok(loyaltyService.getTransactionsByClient(clientId, filterType));
    }

    @Operation(summary = "Obtiene las recompensas canjeadas por un cliente (propio o personal)")
    @GetMapping("/clients/{clientId}/redeemed")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','CLIENT')")
    public ResponseEntity<List<RedeemedRewardDTO>> getClientRedeemedRewards(@PathVariable Long clientId) {
        verifyClientAccess(clientId);
        return ResponseEntity.ok(loyaltyService.getRedeemedRewardsByClient(clientId));
    }

    @Operation(summary = "Registra una adición o resta manual de puntos a un cliente (ADMIN)")
    @PostMapping("/clients/{clientId}/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PointTransactionDTO> adjustClientPoints(
            @PathVariable Long clientId,
            @RequestBody PointAdjustmentRequestDTO request) {
        PointTransactionDTO tx = loyaltyService.adjustPoints(clientId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(tx);
    }

    @Operation(summary = "Lista todos los canjes realizados en el hotel (ADMIN / OPERATOR)")
    @GetMapping("/redeemed")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<List<RedeemedRewardDTO>> getAllRedeemedRewards() {
        return ResponseEntity.ok(loyaltyService.getAllRedeemedRewards());
    }

    @Operation(summary = "Marca una recompensa canjeada como utilizada/entregada (ADMIN / OPERATOR)")
    @PatchMapping("/redeemed/{redemptionId}/use")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<RedeemedRewardDTO> markRedemptionAsUsed(@PathVariable Long redemptionId) {
        return ResponseEntity.ok(loyaltyService.markRedemptionAsUsed(redemptionId));
    }

    // ==================== HELPERS DE SEGURIDAD ====================

    private Long resolveAuthenticatedClientId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Usuario no autenticado.");
        }
        if (auth.getPrincipal() instanceof JwtService.JwtClaims claims && claims.clientId() != null) {
            return claims.clientId();
        }
        if (auth.getName() != null) {
            Client client = clientService.findByUsername(auth.getName());
            if (client != null && client.getId() != null) {
                return client.getId();
            }
        }
        throw new AccessDeniedException("No se pudo identificar al cliente autenticado.");
    }

    private void verifyClientAccess(Long targetClientId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Usuario no autenticado.");
        }
        boolean isStaff = auth.getAuthorities().stream()
                .anyMatch(a -> "ADMIN".equals(a.getAuthority())
                        || "ROLE_ADMIN".equals(a.getAuthority())
                        || "OPERATOR".equals(a.getAuthority())
                        || "ROLE_OPERATOR".equals(a.getAuthority()));
        if (isStaff) {
            return;
        }
        Long authenticatedId = resolveAuthenticatedClientId();
        if (!authenticatedId.equals(targetClientId)) {
            throw new AccessDeniedException("No tiene permiso para consultar la información de fidelidad de otro huésped.");
        }
    }

    private PointTransactionType parseTransactionType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return PointTransactionType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Tipo de transacción inválido. Use EARN, REDEEM, ADJUSTMENT_ADD o ADJUSTMENT_SUBTRACT.");
        }
    }
}
