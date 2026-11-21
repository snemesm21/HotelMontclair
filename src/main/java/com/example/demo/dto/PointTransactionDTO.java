package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO plano de un movimiento en el historial de transacciones de puntos.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointTransactionDTO {

    private Long id;
    private Long clientId;
    private String type;
    private Integer pointsDelta;
    private Integer balanceAfter;
    private String description;
    private Long referenceId;
    private String referenceType;
    private LocalDateTime createdAt;
}
