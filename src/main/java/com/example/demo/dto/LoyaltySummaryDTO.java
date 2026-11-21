package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Resumen integral del estado de fidelidad de un huésped.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltySummaryDTO {

    private Long clientId;
    private String clientName;
    private Integer points;
    private Integer lifetimePoints;
    private String tier;
    private String tierLabel;
    private Double pointsMultiplier;
    private String nextTier;
    private String nextTierLabel;
    private Integer pointsToNextTier;
    private Integer progressPercentage;
    private Integer totalEarned;
    private Integer totalSpent;
    private Long redeemedCount;
}
