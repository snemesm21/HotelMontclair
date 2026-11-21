package com.example.demo.service;

import com.example.demo.dto.RewardDTO;
import com.example.demo.dto.RewardRequestDTO;
import com.example.demo.entities.LoyaltyTier;
import com.example.demo.entities.Reward;
import com.example.demo.errors.NotFoundException;
import com.example.demo.mapper.LoyaltyMapper;
import com.example.demo.repository.RewardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementación del servicio de catálogo de recompensas.
 * Aplica inyección de dependencias por constructor y separa las operaciones de lectura y escritura.
 */
@Service
public class RewardServiceImpl implements RewardService {

    private final RewardRepository rewardRepository;

    public RewardServiceImpl(RewardRepository rewardRepository) {
        this.rewardRepository = rewardRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RewardDTO> getActiveCatalog(String category) {
        List<Reward> rewards = (category != null && !category.isBlank())
                ? rewardRepository.findByActiveTrueAndCategoryIgnoreCaseOrderByPointsCostAsc(category.trim())
                : rewardRepository.findByActiveTrueOrderByPointsCostAsc();
        return rewards.stream()
                .map(LoyaltyMapper::toRewardDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RewardDTO> getAllRewards() {
        return rewardRepository.findAllByOrderByPointsCostAsc().stream()
                .map(LoyaltyMapper::toRewardDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RewardDTO findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID de la recompensa es obligatorio.");
        }
        Reward reward = rewardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(id));
        return LoyaltyMapper.toRewardDTO(reward);
    }

    @Override
    @Transactional
    public RewardDTO createReward(RewardRequestDTO request) {
        validateRewardRequest(request);

        Reward reward = Reward.builder()
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : "")
                .category(normalizeCategory(request.getCategory()))
                .pointsCost(request.getPointsCost())
                .minimumTier(parseTier(request.getMinimumTier()))
                .imageUrl(request.getImageUrl() != null ? request.getImageUrl().trim() : null)
                .stock(request.getStock())
                .active(request.getActive() == null || request.getActive())
                .build();

        return LoyaltyMapper.toRewardDTO(rewardRepository.save(reward));
    }

    @Override
    @Transactional
    public RewardDTO updateReward(Long id, RewardRequestDTO request) {
        if (id == null) {
            throw new IllegalArgumentException("El ID de la recompensa es obligatorio.");
        }
        validateRewardRequest(request);

        Reward existing = rewardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(id));

        existing.setName(request.getName().trim());
        existing.setDescription(request.getDescription() != null ? request.getDescription().trim() : "");
        existing.setCategory(normalizeCategory(request.getCategory()));
        existing.setPointsCost(request.getPointsCost());
        existing.setMinimumTier(parseTier(request.getMinimumTier()));
        existing.setImageUrl(request.getImageUrl() != null ? request.getImageUrl().trim() : existing.getImageUrl());
        existing.setStock(request.getStock());
        if (request.getActive() != null) {
            existing.setActive(request.getActive());
        }

        return LoyaltyMapper.toRewardDTO(rewardRepository.save(existing));
    }

    @Override
    @Transactional
    public void deactivateReward(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID de la recompensa es obligatorio.");
        }
        Reward existing = rewardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(id));
        // Se desactiva en lugar de borrar físicamente para conservar el historial de canjes
        existing.setActive(false);
        rewardRepository.save(existing);
    }

    private void validateRewardRequest(RewardRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos de la recompensa son obligatorios.");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre de la recompensa es obligatorio.");
        }
        if (request.getPointsCost() == null || request.getPointsCost() <= 0) {
            throw new IllegalArgumentException("El costo en puntos debe ser mayor a cero.");
        }
        if (request.getStock() != null && request.getStock() < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo.");
        }
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "EXPERIENCIA";
        }
        return category.trim().toUpperCase();
    }

    private LoyaltyTier parseTier(String tierName) {
        if (tierName == null || tierName.isBlank()) {
            return LoyaltyTier.CLASSIC;
        }
        try {
            return LoyaltyTier.valueOf(tierName.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Categoría de fidelidad inválida. Use CLASSIC, PRIVILEGE o IMPERIAL.");
        }
    }
}
