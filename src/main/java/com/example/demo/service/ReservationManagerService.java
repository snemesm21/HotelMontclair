package com.example.demo.service;

import com.example.demo.entities.Reservation;
import java.util.List;

public interface ReservationManagerService {
    List<Reservation> searchAll();
    Reservation searchById(Long id);
    Reservation save(Reservation reservation);
    void delete(Long id);
}
