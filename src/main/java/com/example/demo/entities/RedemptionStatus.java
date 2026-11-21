package com.example.demo.entities;

/**
 * Estado del ciclo de vida de una recompensa canjeada por el huésped.
 */
public enum RedemptionStatus {
    /** Canjeada y vigente para ser reclamada en el hotel. */
    ACTIVE,
    /** Entregada o disfrutada por el huésped. */
    USED,
    /** Cancelada por el personal del hotel. */
    CANCELLED
}
