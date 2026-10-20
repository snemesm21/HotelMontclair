package com.example.demo.service;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.entities.*;
import com.example.demo.errors.NotFoundException;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.ReservationRoomRepository;
import com.example.demo.repository.RoomTypeRepository;
import com.example.demo.repository.ServiceRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Service
public class ReservationManagerServiceImpl
        implements ReservationManagerService {

    // Constantes de negocio (evitan números mágicos y cadenas literales repetidas)
    private static final int MAX_STAY_NIGHTS = 30;
    private static final int MAX_ADVANCE_YEARS = 1;
    private static final int MIN_GUESTS = 1;
    private static final int MAX_GUESTS = 10;
    private static final Long NEW_RESERVATION_EXCLUDE_ID = -1L;
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

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
    public List<ReservationDTO> getReservationSummaries() {
        return reservationRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private ReservationDTO toDTO(Reservation reservation) {
        ReservationRoom reservationRoom = null;

        if (reservation.getReservationRooms() != null
                && !reservation.getReservationRooms().isEmpty()) {
            reservationRoom = reservation.getReservationRooms().get(0);
        }

        String roomNumber = "";
        String roomName = "";
        String roomTypeName = "";

        double pricePerNight = 0;
        double servicesSubtotal = 0;

        List<ReservationDTO.ServiceDetail> services = new ArrayList<>();

        if (reservationRoom != null) {
            pricePerNight = reservationRoom.getPricePerNight();

            if (reservationRoom.getRoom() != null) {
                roomNumber = reservationRoom.getRoom().getNumber();
                roomName = reservationRoom.getRoom().getName();

                if (reservationRoom.getRoom().getRoomType() != null) {
                    roomTypeName = reservationRoom.getRoom().getRoomType().getName();
                }
            }

            if (reservationRoom.getAcquiredServices() != null) {
                for (AcquiredService acquired : reservationRoom.getAcquiredServices()) {
                    String serviceName = "";

                    if (acquired.getService() != null) {
                        serviceName = acquired.getService().getName();
                    }

                    double subtotal = acquired.getQuantity() * acquired.getUnitPrice();
                    servicesSubtotal += subtotal;

                    services.add(
                            ReservationDTO.ServiceDetail.builder()
                                    .id(acquired.getId())
                                    .name(serviceName)
                                    .quantity(acquired.getQuantity())
                                    .unitPrice(acquired.getUnitPrice())
                                    .subtotal(subtotal)
                                    .build());
                }
            }
        }

        long nights = ChronoUnit.DAYS.between(
                reservation.getCheckInDate(),
                reservation.getCheckOutDate());

        if (nights < 1) {
            nights = 1;
        }

        double roomSubtotal = pricePerNight * nights;
        double totalPrice = roomSubtotal + servicesSubtotal;

        String clientFirstName = "";
        String clientLastName = "";
        String clientEmail = "";
        String clientPhone = "";

        if (reservation.getClient() != null) {
            clientFirstName = reservation.getClient().getFirstName();
            clientLastName = reservation.getClient().getLastName();
            clientEmail = reservation.getClient().getEmail();
            clientPhone = reservation.getClient().getPhone();
        }

        return ReservationDTO.builder()
                .id(reservation.getId())
                .clientFirstName(clientFirstName)
                .clientLastName(clientLastName)
                .clientEmail(clientEmail)
                .clientPhone(clientPhone)
                .roomNumber(roomNumber)
                .roomName(roomName)
                .roomTypeName(roomTypeName)
                .pricePerNight(pricePerNight)
                .checkInDate(reservation.getCheckInDate())
                .checkOutDate(reservation.getCheckOutDate())
                .numberOfPeople(reservation.getNumberOfPeople())
                .status(reservation.getStatus())
                .nights(nights)
                .roomSubtotal(roomSubtotal)
                .servicesSubtotal(servicesSubtotal)
                .totalPrice(totalPrice)
                .services(services)
                .build();
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

    private void validateNumberOfPeople(int numberOfPeople) {
        if (numberOfPeople < MIN_GUESTS) {
            throw new IllegalArgumentException(
                    "El número de huéspedes debe ser de al menos " + MIN_GUESTS + " persona.");
        }

        if (numberOfPeople > MAX_GUESTS) {
            throw new IllegalArgumentException(
                    "El número máximo de huéspedes permitido es de " + MAX_GUESTS + " personas por habitación.");
        }
    }

    private void validateDates(
            LocalDate checkInDate,
            LocalDate checkOutDate) {
        validateDateRules(checkInDate, checkOutDate);

        if (checkInDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La fecha de entrada no puede ser anterior a la fecha actual.");
        }
    }

    private void validateDatesForUpdate(
            LocalDate checkInDate,
            LocalDate checkOutDate,
            LocalDate originalCheckIn) {
        validateDateRules(checkInDate, checkOutDate);

        if (!checkInDate.equals(originalCheckIn) && checkInDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La nueva fecha de entrada no puede ser anterior a la fecha actual.");
        }
    }

    private void validateDateRules(
            LocalDate checkInDate,
            LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            throw new IllegalArgumentException(
                    "Las fechas de entrada y salida son obligatorias.");
        }

        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException(
                    "La fecha de salida debe ser posterior a la fecha de entrada (mínimo 1 noche de estancia).");
        }

        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (nights > MAX_STAY_NIGHTS) {
            throw new IllegalArgumentException(
                    "La estancia máxima permitida es de " + MAX_STAY_NIGHTS + " noches consecutivas.");
        }

        if (checkInDate.isAfter(LocalDate.now().plusYears(MAX_ADVANCE_YEARS))) {
            throw new IllegalArgumentException(
                    "No se pueden realizar reservas con más de un año de anticipación.");
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

        if (!roomTypeRepository.existsById(roomTypeId)) {
            throw new IllegalArgumentException(
                    "El tipo de habitación seleccionado no existe.");
        }

        validateNumberOfPeople(numberOfPeople);
        validateDateRules(checkInDate, checkOutDate);

        // Consulta 1: habitaciones candidatas del tipo solicitado y con capacidad
        List<Room> candidateRooms = roomRepository
                .findByRoomType_IdAndStatusAndCapacityGreaterThanEqualOrderByIdAsc(
                        roomTypeId,
                        RoomStatus.AVAILABLE,
                        numberOfPeople);

        if (candidateRooms.isEmpty()) {
            throw new IllegalArgumentException(
                    "No hay habitaciones disponibles de este tipo con capacidad suficiente ("
                            + numberOfPeople + " personas) o en estado DISPONIBLE.");
        }

        // Consulta 2: habitaciones ocupadas en el rango de fechas
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
                    "No hay disponibilidad para el tipo de habitación seleccionado "
                            + "en las fechas solicitadas.");
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

        if (serviceIds == null || serviceIds.isEmpty()) {
            return;
        }

        Set<Long> uniqueIds = new HashSet<>();
        for (Long serviceId : serviceIds) {
            if (serviceId != null && !uniqueIds.add(serviceId)) {
                throw new IllegalArgumentException(
                        "No se pueden agregar servicios adicionales duplicados (ID: " + serviceId + ").");
            }
        }

        for (Long serviceId : serviceIds) {
            if (serviceId == null) {
                continue;
            }

            com.example.demo.entities.Service service =
                    serviceRepository.findById(serviceId)
                            .orElseThrow(
                                    () -> new IllegalArgumentException(
                                            "El servicio con ID "
                                                    + serviceId
                                                    + " no existe."));

            if (service.isHidden()) {
                throw new IllegalArgumentException(
                        "El servicio '" + service.getName() + "' no se encuentra disponible actualmente.");
            }

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

        validateDates(checkInDate, checkOutDate);
        validateNumberOfPeople(numberOfPeople);

        Room room = findAvailableRoom(
                roomTypeId,
                checkInDate,
                checkOutDate,
                numberOfPeople,
                NEW_RESERVATION_EXCLUDE_ID,
                null);

        Reservation reservation = Reservation.builder()
                .client(client)
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .numberOfPeople(numberOfPeople)
                .status(STATUS_CONFIRMED)
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

        if (id == null) {
            throw new IllegalArgumentException("El ID de la reserva es obligatorio.");
        }

        Reservation reservation =
                reservationRepository.findById(id).orElse(null);

        if (reservation == null) {
            return null;
        }

        if (STATUS_CANCELLED.equalsIgnoreCase(reservation.getStatus())) {
            throw new IllegalArgumentException(
                    "No se puede modificar una reserva que ha sido cancelada.");
        }

        if (reservation.getCheckOutDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "No se puede modificar una reserva cuya estancia ya ha finalizado.");
        }

        LocalDate finalCheckIn = checkInDate != null
                ? checkInDate
                : reservation.getCheckInDate();

        LocalDate finalCheckOut = checkOutDate != null
                ? checkOutDate
                : reservation.getCheckOutDate();

        if (checkInDate != null && !checkInDate.equals(reservation.getCheckInDate())) {
            if (reservation.getCheckInDate().isBefore(LocalDate.now())) {
                throw new IllegalArgumentException(
                        "No se puede modificar la fecha de entrada de una reserva que ya ha comenzado.");
            }
        }

        validateDatesForUpdate(finalCheckIn, finalCheckOut, reservation.getCheckInDate());

        int finalNumberOfPeople = numberOfPeople > 0
                ? numberOfPeople
                : reservation.getNumberOfPeople();

        validateNumberOfPeople(finalNumberOfPeople);

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
        if (id == null) {
            throw new IllegalArgumentException("El ID de la reserva es obligatorio.");
        }

        Reservation reservation =
                reservationRepository.findById(id).orElse(null);

        if (reservation != null) {
            reservationRepository.delete(reservation);
        }
    }
}