package com.example.demo.mapper;

import com.example.demo.dto.PointTransactionDTO;
import com.example.demo.dto.RedeemedRewardDTO;
import com.example.demo.dto.RewardDTO;
import com.example.demo.entities.Client;
import com.example.demo.entities.LoyaltyTier;
import com.example.demo.entities.PointTransaction;
import com.example.demo.entities.RedeemedReward;
import com.example.demo.entities.Reward;

/**
 * Mapper del módulo de fidelidad y recompensas.
 * Convierte entidades JPA en DTOs planos evitando exponer grafos anidados circulares.
 */
public final class LoyaltyMapper {

    private LoyaltyMapper() {
    }

    public static RewardDTO toRewardDTO(Reward reward) {
        if (reward == null) {
            return null;
        }
        LoyaltyTier minTier = reward.getMinimumTier() != null ? reward.getMinimumTier() : LoyaltyTier.CLASSIC;
        return RewardDTO.builder()
                .id(reward.getId())
                .name(reward.getName())
                .description(reward.getDescription())
                .category(reward.getCategory())
                .pointsCost(reward.getPointsCost())
                .minimumTier(minTier.name())
                .minimumTierLabel(minTier.getDisplayName())
                .imageUrl(reward.getImageUrl())
                .stock(reward.getStock())
                .active(reward.isActive())
                .available(reward.isActive() && reward.hasAvailableStock())
                .build();
    }

    public static RedeemedRewardDTO toRedeemedRewardDTO(RedeemedReward redeemed) {
        if (redeemed == null) {
            return null;
        }
        Client client = redeemed.getClient();
        Reward reward = redeemed.getReward();

        return RedeemedRewardDTO.builder()
                .id(redeemed.getId())
                .clientId(client != null ? client.getId() : null)
                .clientName(formatFullName(client))
                .rewardId(reward != null ? reward.getId() : null)
                .rewardName(reward != null ? reward.getName() : "")
                .rewardDescription(reward != null ? reward.getDescription() : "")
                .rewardCategory(reward != null ? reward.getCategory() : "")
                .rewardImageUrl(reward != null ? reward.getImageUrl() : null)
                .pointsSpent(redeemed.getPointsSpent())
                .redemptionCode(redeemed.getRedemptionCode())
                .status(redeemed.getStatus() != null ? redeemed.getStatus().name() : null)
                .redeemedAt(redeemed.getRedeemedAt())
                .usedAt(redeemed.getUsedAt())
                .build();
    }

    public static PointTransactionDTO toPointTransactionDTO(PointTransaction tx) {
        if (tx == null) {
            return null;
        }
        return PointTransactionDTO.builder()
                .id(tx.getId())
                .clientId(tx.getClient() != null ? tx.getClient().getId() : null)
                .type(tx.getType() != null ? tx.getType().name() : null)
                .pointsDelta(tx.getPointsDelta())
                .balanceAfter(tx.getBalanceAfter())
                .description(tx.getDescription())
                .referenceId(tx.getReferenceId())
                .referenceType(tx.getReferenceType())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    public static String formatFullName(Client client) {
        if (client == null) {
            return "";
        }
        String first = client.getFirstName() != null ? client.getFirstName().trim() : "";
        String last = client.getLastName() != null ? client.getLastName().trim() : "";
        return (first + " " + last).trim();
    }
}
