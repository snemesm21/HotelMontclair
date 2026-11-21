package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload de entrada para realizar adiciones o restas manuales de puntos (ADMIN).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointAdjustmentRequestDTO {

    /** Cantidad positiva de puntos a sumar o restar. */
    private Integer points;

    /** Operación deseada: "ADD" o "SUBTRACT". */
    private String operation;

    /** Motivo o concepto visible en el historial de transacciones. */
    private String description;
}
