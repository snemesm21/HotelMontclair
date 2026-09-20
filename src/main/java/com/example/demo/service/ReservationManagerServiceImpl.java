package com.example.demo.service;

import com.example.demo.entities.Reservation;
import com.example.demo.errors.NotFoundException;
import com.example.demo.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ReservationManagerServiceImpl implements ReservationManagerService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Override
    public List<Reservation> searchAll() {
        return reservationRepository.findAll();
    }

    @Override
    public Reservation searchById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(id));
    }

    @Override
    public Reservation save(Reservation reservation) {
        return reservationRepository.save(reservation);
    }

    @Override
    public void delete(Long id) {
        if (!reservationRepository.existsById(id)) {
            throw new NotFoundException(id);
        }
        reservationRepository.deleteById(id);
    }
}
