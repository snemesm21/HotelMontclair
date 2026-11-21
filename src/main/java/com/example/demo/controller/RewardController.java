package com.example.demo.controller;

import com.example.demo.dto.RewardDTO;
import com.example.demo.dto.RewardRequestDTO;
import com.example.demo.service.RewardService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST del catálogo de recompensas (/api/rewards).
 * Solo interactúa con la capa de servicio (RewardService).
 */
@RestController
@RequestMapping("/api/rewards")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class RewardController {

    private final RewardService rewardService;

    public RewardController(RewardService rewardService) {
        this.rewardService = rewardService;
    }

    @Operation(summary = "Obtiene el catálogo activo de recompensas (con filtro opcional por categoría)")
    @GetMapping
    public List<RewardDTO> getActiveCatalog(@RequestParam(required = false) String category) {
        return rewardService.getActiveCatalog(category);
    }

    @Operation(summary = "Obtiene todas las recompensas del catálogo, incluyendo inactivas (ADMIN)")
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public List<RewardDTO> getAllRewardsAdmin() {
        return rewardService.getAllRewards();
    }

    @Operation(summary = "Obtiene el detalle de una recompensa por su ID")
    @GetMapping("/{id}")
    public ResponseEntity<RewardDTO> getRewardById(@PathVariable Long id) {
        return ResponseEntity.ok(rewardService.findById(id));
    }

    @Operation(summary = "Crea una nueva recompensa en el catálogo (ADMIN)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RewardDTO> createReward(@RequestBody RewardRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rewardService.createReward(request));
    }

    @Operation(summary = "Actualiza una recompensa existente del catálogo (ADMIN)")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RewardDTO> updateReward(
            @PathVariable Long id,
            @RequestBody RewardRequestDTO request) {
        return ResponseEntity.ok(rewardService.updateReward(id, request));
    }

    @Operation(summary = "Desactiva una recompensa del catálogo preservando el historial de canjes (ADMIN)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateReward(@PathVariable Long id) {
        rewardService.deactivateReward(id);
        return ResponseEntity.noContent().build();
    }
}
