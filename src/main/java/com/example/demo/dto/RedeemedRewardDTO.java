package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO plano de una recompensa canjeada por el huésped (sección "Canjeados").
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedeemedRewardDTO {

    private Long id;
    private Long clientId;
    private String clientName;
    private Long rewardId;
    private String rewardName;
    private String rewardDescription;
    private String rewardCategory;
    private String rewardImageUrl;
    private Integer pointsSpent;
    private String redemptionCode;
    private String status;
    private LocalDateTime redeemedAt;
    private LocalDateTime usedAt;
}
