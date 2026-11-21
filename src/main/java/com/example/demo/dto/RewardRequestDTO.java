package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload de entrada para crear o actualizar una recompensa del catálogo (ADMIN).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardRequestDTO {

    private String name;
    private String description;
    private String category;
    private Integer pointsCost;
    private String minimumTier;
    private String imageUrl;
    private Integer stock;
    private Boolean active;
}
