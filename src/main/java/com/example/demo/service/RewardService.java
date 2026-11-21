package com.example.demo.service;

import com.example.demo.dto.RewardDTO;
import com.example.demo.dto.RewardRequestDTO;

import java.util.List;

/**
 * Servicio de gestión del catálogo de recompensas del programa de fidelidad.
 */
public interface RewardService {

    List<RewardDTO> getActiveCatalog(String category);

    List<RewardDTO> getAllRewards();

    RewardDTO findById(Long id);

    RewardDTO createReward(RewardRequestDTO request);

    RewardDTO updateReward(Long id, RewardRequestDTO request);

    void deactivateReward(Long id);
}
