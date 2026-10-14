package com.example.demo.service;

import com.example.demo.entities.*;
import com.example.demo.errors.NotFoundException;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReservationManagerServiceImpl implements ReservationManagerService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Override
    public List<Reservation> searchAll() {
        return reservationRepository.findAll();
    }

    @Override
    public Reservation searchById(Long id) {
        return reservationRepository.findById(id).orElse(null);
    }

    @Override
    public List<Reservation> searchByClientId(Long clientId) {
        if (clientId == null) return List.of();
        return reservationRepository.findByClientId(clientId);
    }

    @Override
    @Transactional
    public Reservation save(Reservation reservation) {
        Reservation saved = reservationRepository.save(reservation);

        if (saved.getReservationRooms() != null) {
            for (ReservationRoom rr : saved.getReservationRooms()) {
                Room room = rr.getRoom();
                if (room != null && room.getStatus() != RoomStatus.OCCUPIED) {
                    room.setStatus(RoomStatus.OCCUPIED);
                    roomRepository.save(room);
                }
            }
        }
        return saved;
    }

    @Override
    @Transactional
    public Reservation createReservation(Long clientId, Long roomId, LocalDate checkInDate, LocalDate checkOutDate, int numberOfPeople, List<Long> serviceIds) {
        if (clientId == null) throw new IllegalArgumentException("El cliente es obligatorio.");
        if (roomId == null) throw new IllegalArgumentException("La habitación es obligatoria.");
        if (checkInDate == null || checkOutDate == null) throw new IllegalArgumentException("Las fechas son obligatorias.");
        if (!checkOutDate.isAfter(checkInDate)) throw new IllegalArgumentException("La fecha de salida debe ser posterior a la de entrada.");

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException(clientId));
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException(roomId));

        Reservation reservation = Reservation.builder()
                .client(client)
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .numberOfPeople(numberOfPeople > 0 ? numberOfPeople : 1)
                .status("CONFIRMED")
                .reservationRooms(new ArrayList<>())
                .build();

        ReservationRoom resRoom = ReservationRoom.builder()
                .room(room)
                .pricePerNight(room.getPricePerNight())
                .acquiredServices(new ArrayList<>())
                .build();

        if (serviceIds != null) {
            for (Long sId : serviceIds) {
                com.example.demo.entities.Service svc = serviceRepository.findById(sId).orElse(null);
                if (svc != null) {
                    AcquiredService acq = AcquiredService.builder()
                            .service(svc)
                            .date(checkInDate)
                            .quantity(1)
                            .unitPrice(svc.getPrice() != null ? svc.getPrice() : 0.0)
                            .build();
                    resRoom.addAcquiredService(acq);
                }
            }
        }

        reservation.addReservationRoom(resRoom);
        room.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);

        return reservationRepository.save(reservation);
    }

    @Override
    @Transactional
    public Reservation updateReservation(Long id, Long clientId, Long roomId, LocalDate checkInDate, LocalDate checkOutDate, int numberOfPeople, List<Long> serviceIds) {
        Reservation reservation = searchById(id);
        if (reservation == null) {
            return null;
        }

        if (clientId != null) {
            Client client = clientRepository.findById(clientId).orElse(null);
            if (client != null) {
                reservation.setClient(client);
            }
        }

        if (checkInDate != null) reservation.setCheckInDate(checkInDate);
        if (checkOutDate != null) reservation.setCheckOutDate(checkOutDate);
        if (numberOfPeople > 0) reservation.setNumberOfPeople(numberOfPeople);

        ReservationRoom resRoom;
        if (reservation.getReservationRooms() != null && !reservation.getReservationRooms().isEmpty()) {
            resRoom = reservation.getReservationRooms().get(0);
        } else {
            resRoom = ReservationRoom.builder().acquiredServices(new ArrayList<>()).build();
            reservation.addReservationRoom(resRoom);
        }

        if (roomId != null) {
            Room currentRoom = resRoom.getRoom();
            if (currentRoom == null || !currentRoom.getId().equals(roomId)) {
                if (currentRoom != null) {
                    currentRoom.setStatus(RoomStatus.AVAILABLE);
                    roomRepository.save(currentRoom);
                }
                Room newRoom = roomRepository.findById(roomId).orElse(null);
                if (newRoom != null) {
                    newRoom.setStatus(RoomStatus.OCCUPIED);
                    roomRepository.save(newRoom);
                    resRoom.setRoom(newRoom);
                    resRoom.setPricePerNight(newRoom.getPricePerNight());
                }
            }
        }

        if (serviceIds != null) {
            if (resRoom.getAcquiredServices() != null) {
                resRoom.getAcquiredServices().clear();
            } else {
                resRoom.setAcquiredServices(new ArrayList<>());
            }

            LocalDate svcDate = reservation.getCheckInDate() != null ? reservation.getCheckInDate() : LocalDate.now();
            for (Long sId : serviceIds) {
                com.example.demo.entities.Service svc = serviceRepository.findById(sId).orElse(null);
                if (svc != null) {
                    AcquiredService acq = AcquiredService.builder()
                            .service(svc)
                            .date(svcDate)
                            .quantity(1)
                            .unitPrice(svc.getPrice() != null ? svc.getPrice() : 0.0)
                            .build();
                    resRoom.addAcquiredService(acq);
                }
            }
        }

        return reservationRepository.save(reservation);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Reservation reservation = reservationRepository.findById(id).orElse(null);
        if (reservation != null) {
            if (reservation.getReservationRooms() != null) {
                for (ReservationRoom rr : reservation.getReservationRooms()) {
                    Room room = rr.getRoom();
                    if (room != null) {
                        room.setStatus(RoomStatus.AVAILABLE);
                        roomRepository.save(room);
                    }
                }
            }
            reservationRepository.delete(reservation);
        }
    }
}
