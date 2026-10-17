
package com.example.demo.service;

import com.example.demo.entities.Reservation;

import java.time.LocalDate;
import java.util.List;

public interface ReservationManagerService {

    List<Reservation> searchAll();

    Reservation searchById(Long id);

    List<Reservation> searchByClientId(Long clientId);

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
}
