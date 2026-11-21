package com.example.demo.service;

import com.example.demo.dto.LoyaltySummaryDTO;
import com.example.demo.dto.PointAdjustmentRequestDTO;
import com.example.demo.dto.PointTransactionDTO;
import com.example.demo.dto.RedeemedRewardDTO;
import com.example.demo.entities.PointTransactionType;
import com.example.demo.entities.Reservation;

import java.util.List;

/**
 * Servicio principal del sistema de puntos, categorías de fidelidad,
 * canje de recompensas y libro mayor de transacciones.
 */
public interface LoyaltyService {

    LoyaltySummaryDTO getClientLoyaltySummary(Long clientId);

    List<PointTransactionDTO> getTransactionsByClient(Long clientId, PointTransactionType type);

    List<RedeemedRewardDTO> getRedeemedRewardsByClient(Long clientId);

    List<RedeemedRewardDTO> getAllRedeemedRewards();

    RedeemedRewardDTO redeemReward(Long clientId, Long rewardId);

    RedeemedRewardDTO markRedemptionAsUsed(Long redeemedRewardId);

    PointTransactionDTO adjustPoints(Long clientId, PointAdjustmentRequestDTO request);

    PointTransactionDTO awardPointsForReservation(Reservation reservation, double totalAmount);

    PointTransactionDTO grantWelcomeBonus(Long clientId);
}
