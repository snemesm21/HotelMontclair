package com.example.demo.service;

import com.example.demo.entities.*;
import com.example.demo.errors.NotFoundException;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.ReservationRoomRepository;
import com.example.demo.repository.ServiceRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Service
public class ReservationManagerServiceImpl
        implements ReservationManagerService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ReservationRoomRepository reservationRoomRepository;

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
        if (clientId == null) {
            return List.of();
        }

        return reservationRepository.findByClientId(clientId);
    }

    @Override
    @Transactional
    public Reservation save(Reservation reservation) {
        return reservationRepository.save(reservation);
    }

    private void validateDates(
            LocalDate checkInDate,
            LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            throw new IllegalArgumentException(
                    "Las fechas son obligatorias.");
        }

        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException(
                    "La fecha de salida debe ser posterior a la de entrada.");
        }
    }

    private Room findAvailableRoom(
            Long roomTypeId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int numberOfPeople,
            Long excludeReservationId,
            Long preferredRoomId) {

        if (roomTypeId == null) {
            throw new IllegalArgumentException(
                    "El tipo de habitación es obligatorio.");
        }

        if (numberOfPeople < 1) {
            throw new IllegalArgumentException(
                    "Debe haber al menos un huésped.");
        }

        validateDates(checkInDate, checkOutDate);

        // Consulta 1: habitaciones candidatas
        List<Room> candidateRooms = roomRepository
                .findByRoomType_IdAndStatusAndCapacityGreaterThanEqualOrderByIdAsc(
                        roomTypeId,
                        RoomStatus.AVAILABLE,
                        numberOfPeople);

        // Consulta 2: habitaciones ocupadas
        Set<Long> occupiedRoomIds = new HashSet<>(
                reservationRoomRepository.findOccupiedRoomIds(
                        checkInDate,
                        checkOutDate,
                        excludeReservationId));

        // Filtrar habitaciones ocupadas
        List<Room> availableRooms = new ArrayList<>();

        for (Room room : candidateRooms) {
            if (!occupiedRoomIds.contains(room.getId())) {
                availableRooms.add(room);
            }
        }

        if (availableRooms.isEmpty()) {
            throw new IllegalArgumentException(
                    "No hay habitaciones disponibles de este tipo "
                            + "para las fechas y cantidad de huéspedes seleccionados.");
        }

        // Priorizar la habitación actual si sigue disponible
        if (preferredRoomId != null) {
            for (Room room : availableRooms) {
                if (preferredRoomId.equals(room.getId())) {
                    return room;
                }
            }
        }

        return availableRooms.get(0);
    }

    private void addServices(
            ReservationRoom reservationRoom,
            List<Long> serviceIds,
            LocalDate checkInDate) {

        if (serviceIds == null) {
            return;
        }

        for (Long serviceId : serviceIds) {

            com.example.demo.entities.Service service =
                    serviceRepository.findById(serviceId)
                            .orElseThrow(
                                    () -> new IllegalArgumentException(
                                            "El servicio con ID "
                                                    + serviceId
                                                    + " no existe."));

            AcquiredService acquiredService =
                    AcquiredService.builder()
                            .service(service)
                            .date(checkInDate)
                            .quantity(1)
                            .unitPrice(
                                    service.getPrice() != null
                                            ? service.getPrice()
                                            : 0.0)
                            .build();

            reservationRoom.addAcquiredService(acquiredService);
        }
    }

    @Override
    @Transactional
    public Reservation createReservation(
            Long clientId,
            Long roomTypeId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int numberOfPeople,
            List<Long> serviceIds) {

        if (clientId == null) {
            throw new IllegalArgumentException(
                    "El cliente es obligatorio.");
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(
                        () -> new NotFoundException(clientId));

        Room room = findAvailableRoom(
                roomTypeId,
                checkInDate,
                checkOutDate,
                numberOfPeople,
                -1L,
                null);

        Reservation reservation = Reservation.builder()
                .client(client)
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .numberOfPeople(numberOfPeople)
                .status("CONFIRMED")
                .reservationRooms(new ArrayList<>())
                .build();

        ReservationRoom reservationRoom =
                ReservationRoom.builder()
                        .room(room)
                        .pricePerNight(room.getPricePerNight())
                        .acquiredServices(new ArrayList<>())
                        .build();

        addServices(
                reservationRoom,
                serviceIds,
                checkInDate);

        reservation.addReservationRoom(reservationRoom);

        return reservationRepository.save(reservation);
    }

    @Override
    @Transactional
    public Reservation updateReservation(
            Long id,
            Long clientId,
            Long roomTypeId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int numberOfPeople,
            List<Long> serviceIds) {

        Reservation reservation =
                reservationRepository.findById(id).orElse(null);

        if (reservation == null) {
            return null;
        }

        LocalDate finalCheckIn = checkInDate != null
                ? checkInDate
                : reservation.getCheckInDate();

        LocalDate finalCheckOut = checkOutDate != null
                ? checkOutDate
                : reservation.getCheckOutDate();

        int finalNumberOfPeople = numberOfPeople > 0
                ? numberOfPeople
                : reservation.getNumberOfPeople();

        validateDates(finalCheckIn, finalCheckOut);

        ReservationRoom reservationRoom;

        if (reservation.getReservationRooms() != null
                && !reservation.getReservationRooms().isEmpty()) {

            reservationRoom =
                    reservation.getReservationRooms().get(0);

        } else {
            reservationRoom = ReservationRoom.builder()
                    .acquiredServices(new ArrayList<>())
                    .build();

            reservation.addReservationRoom(reservationRoom);
        }

        Room currentRoom = reservationRoom.getRoom();

        Long finalRoomTypeId = roomTypeId;

        if (finalRoomTypeId == null && currentRoom != null) {
            finalRoomTypeId = currentRoom.getRoomType().getId();
        }

        Room selectedRoom = findAvailableRoom(
                finalRoomTypeId,
                finalCheckIn,
                finalCheckOut,
                finalNumberOfPeople,
                id,
                currentRoom != null ? currentRoom.getId() : null);

        if (clientId != null) {
            Client client = clientRepository.findById(clientId)
                    .orElseThrow(
                            () -> new NotFoundException(clientId));

            reservation.setClient(client);
        }

        reservation.setCheckInDate(finalCheckIn);
        reservation.setCheckOutDate(finalCheckOut);
        reservation.setNumberOfPeople(finalNumberOfPeople);

        if (currentRoom == null
                || !currentRoom.getId().equals(selectedRoom.getId())) {

            reservationRoom.setRoom(selectedRoom);

            reservationRoom.setPricePerNight(
                    selectedRoom.getPricePerNight());
        }

        if (serviceIds != null) {

            if (reservationRoom.getAcquiredServices() == null) {
                reservationRoom.setAcquiredServices(
                        new ArrayList<>());
            } else {
                reservationRoom.getAcquiredServices().clear();
            }

            addServices(
                    reservationRoom,
                    serviceIds,
                    finalCheckIn);
        }

        return reservationRepository.save(reservation);
    }

    @Override
    @Transactional
    public void delete(Long id) {

        Reservation reservation =
                reservationRepository.findById(id).orElse(null);

        if (reservation != null) {
            reservationRepository.delete(reservation);
        }
    }
}