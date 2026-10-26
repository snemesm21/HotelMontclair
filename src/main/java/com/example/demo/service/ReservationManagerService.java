package com.example.demo.service;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.entities.Reservation;

import java.time.LocalDate;
import java.util.List;

/**
 * Interfaz del servicio de gestión de reservas del hotel.
 * Define la lógica de negocio para crear, actualizar, cancelar,
 * activar estancias, liquidar cuentas y finalizar estancias.
 */
public interface ReservationManagerService {

    List<Reservation> searchAll();

    List<ReservationDTO> getReservationSummaries();

    Reservation searchById(Long id);

    ReservationDTO findSummaryById(Long id);

    List<Reservation> searchByClientId(Long clientId);

    List<ReservationDTO> getSummariesByClientId(Long clientId);

    Reservation save(Reservation reservation);

    Reservation createReservation(
            Long clientId,
            Long roomTypeId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int numberOfPeople,
            List<Long> serviceIds);

    Reservation updateReservation(
            Long id,
            Long clientId,
            Long roomTypeId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int numberOfPeople,
            List<Long> serviceIds);

    void delete(Long id);

    /**
     * Cancela una reserva (cambia su estado a CANCELLED).
     * No la elimina físicamente, permitiendo que el usuario conserve su historial.
     */
    ReservationDTO cancelReservation(Long id);

    /**
     * Activa la estancia del cliente cuando este llega al hotel (estado ACTIVE).
     */
    ReservationDTO activateStay(Long id);

    /**
     * Finaliza la estancia del cliente (estado INACTIVE).
     * REGLA DE NEGOCIO: Solo se permite si la cuenta está vacía/pagada (paid == true).
     */
    ReservationDTO finishStay(Long id);

    /**
     * Registra el pago total de la cuenta de la estancia (paid = true).
     * Limpia la cuenta del cliente dejando el saldo pendiente en 0.
     */
    ReservationDTO payStayAccount(Long id);
}
