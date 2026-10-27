package com.example.demo.service;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.entities.Reservation;

import java.time.LocalDate;
import java.util.List;

/**
 * Interfaz del servicio de gestion de reservas del hotel.
 * Define la logica de negocio para crear, actualizar, cancelar,
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
     * No la elimina fisicamente, permitiendo que el usuario conserve su historial.
     * REGLA DE NEGOCIO 4: Solo se pueden cancelar reservas que todavia no estan activas,
     * es decir que su fecha de inicio es posterior a la fecha actual.
     */
    ReservationDTO cancelReservation(Long id);

    /**
     * Activa la estancia del cliente cuando este llega al hotel (estado ACTIVE).
     * REGLA DE NEGOCIO: No se puede activar si la habitacion ya tiene otra estadia activa.
     */
    ReservationDTO activateStay(Long id);

    /**
     * Finaliza la estancia del cliente (estado INACTIVE).
     * REGLA DE NEGOCIO: Solo se permite si la cuenta esta vacia/pagada (paid == true).
     */
    ReservationDTO finishStay(Long id);

    /**
     * Registra el pago total de la cuenta de la estancia (paid = true).
     * Limpia la cuenta del cliente dejando el saldo pendiente en 0.
     */
    ReservationDTO payStayAccount(Long id);

    /**
     * Asigna un servicio consumido a una habitacion durante la estancia.
     * REGLA DE NEGOCIO 5: En caso de que en la habitacion no haya ninguna reserva activa,
     * no se debe poder asignar ningun servicio.
     */
    ReservationDTO assignServiceToRoom(Long roomId, Long serviceId, int quantity);

    /**
     * Obtiene la reserva activa asociada al numero de habitacion (e.g. "101").
     * Devuelve el DTO de la unica estadia actualmente activa en esa habitacion.
     */
    ReservationDTO findActiveSummaryByRoomNumber(String roomNumber);

    /**
     * Elimina un servicio contratado de la cuenta de una reserva.
     */
    ReservationDTO removeServiceFromReservation(Long reservationId, Long acquiredServiceId);
}
