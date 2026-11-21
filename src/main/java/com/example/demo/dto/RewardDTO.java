package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO plano de una recompensa del catálogo.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardDTO {

    private Long id;
    private String name;
    private String description;
    private String category;
    private Integer pointsCost;
    private String minimumTier;
    private String minimumTierLabel;
    private String imageUrl;
    private Integer stock;
    private boolean active;
    private boolean available;
}
