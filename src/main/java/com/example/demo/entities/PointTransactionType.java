package com.example.demo.entities;

/**
 * Tipos de movimientos registrados en el historial de transacciones de puntos.
 */
public enum PointTransactionType {
    /** Adición automática de puntos (estancia pagada, bono de bienvenida). */
    EARN,
    /** Resta de puntos por canje de una recompensa del catálogo. */
    REDEEM,
    /** Adición manual realizada por un administrador (bonificación / cortesía). */
    ADJUSTMENT_ADD,
    /** Resta manual realizada por un administrador (corrección / ajuste). */
    ADJUSTMENT_SUBTRACT
}
