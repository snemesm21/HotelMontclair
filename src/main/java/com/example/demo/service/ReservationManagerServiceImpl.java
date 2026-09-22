package com.example.demo.service;

import com.example.demo.entities.Reservation;
import com.example.demo.entities.ReservationRoom;
import com.example.demo.entities.Room;
import com.example.demo.entities.RoomStatus;
import com.example.demo.errors.NotFoundException;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.RoomRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ReservationManagerServiceImpl implements ReservationManagerService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RoomRepository roomRepository;

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
        Reservation saved = reservationRepository.save(reservation);

        for (ReservationRoom rr : saved.getReservationRooms()) {
            Room room = rr.getRoom();
            if (room != null && room.getStatus() != RoomStatus.OCCUPIED) {
                room.setStatus(RoomStatus.OCCUPIED);
                roomRepository.save(room);
            }
        }
        return saved;
    }

    @Override
    public void delete(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(id));
            
        for (ReservationRoom rr : reservation.getReservationRooms()) {
            Room room = rr.getRoom();
            if (room != null) {
                room.setStatus(RoomStatus.AVAILABLE);
                roomRepository.save(room);
            }
        }
        reservationRepository.delete(reservation);
    }
}
