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

/**
 * Implementación del servicio de gestión de reservas del hotel.
 * Coordina las entidades Client, Room, RoomType, Reservation, ReservationRoom y Service.
 * Cumple con la arquitectura en capas y evita devolver grafos anidados circulares usando ReservationDTO.
 */
@Service
public class ReservationManagerServiceImpl
        implements ReservationManagerService {

    // Constantes de negocio
    private static final int MAX_STAY_NIGHTS = 30;
    private static final int MAX_ADVANCE_YEARS = 1;
    private static final int MIN_GUESTS = 1;
    private static final int MAX_GUESTS = 10;
    private static final Long NEW_RESERVATION_EXCLUDE_ID = -1L;

    // Estados de ciclo de vida de la reserva
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_CANCELLED = "CANCELLED";

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

    @Override
    public Reservation searchById(Long id) {
        return reservationRepository.findById(id).orElse(null);
    }

    @Override
    public ReservationDTO findSummaryById(Long id) {
        if (id == null) {
            return null;
        }
        Reservation reservation = reservationRepository.findById(id).orElse(null);
        return reservation != null ? toDTO(reservation) : null;
    }

    @Override
    public List<Reservation> searchByClientId(Long clientId) {
        if (clientId == null) {
            return List.of();
        }
        return reservationRepository.findByClientId(clientId);
    }

    @Override
    public List<ReservationDTO> getSummariesByClientId(Long clientId) {
        if (clientId == null) {
            return List.of();
        }
        return reservationRepository.findByClientId(clientId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Mapea una entidad Reservation a su correspondiente ReservationDTO plano.
     * Calcula noches, subtotales, saldo pendiente y estado financiero de la cuenta.
     */
    private ReservationDTO toDTO(Reservation reservation) {
        ReservationRoom reservationRoom = null;

        if (reservation.getReservationRooms() != null
                && !reservation.getReservationRooms().isEmpty()) {
            reservationRoom = reservation.getReservationRooms().get(0);
        }

        Long roomId = null;
        String roomNumber = "";
        String roomName = "";
        String roomTypeName = "";
        Long roomTypeId = null;

        double pricePerNight = 0;
        double servicesSubtotal = 0;

        List<ReservationDTO.ServiceDetail> services = new ArrayList<>();
        List<ReservationDTO.AcquiredServiceSummary> acquiredSummaries = new ArrayList<>();

        if (reservationRoom != null) {
            pricePerNight = reservationRoom.getPricePerNight();

            if (reservationRoom.getRoom() != null) {
                roomId = reservationRoom.getRoom().getId();
                roomNumber = reservationRoom.getRoom().getNumber();
                roomName = reservationRoom.getRoom().getName();

                if (reservationRoom.getRoom().getRoomType() != null) {
                    roomTypeId = reservationRoom.getRoom().getRoomType().getId();
                    roomTypeName = reservationRoom.getRoom().getRoomType().getName();
                }
            }

            if (reservationRoom.getAcquiredServices() != null) {
                for (AcquiredService acquired : reservationRoom.getAcquiredServices()) {
                    String serviceName = "";
                    Long sId = null;

                    if (acquired.getService() != null) {
                        sId = acquired.getService().getId();
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

                    acquiredSummaries.add(
                            ReservationDTO.AcquiredServiceSummary.builder()
                                    .id(acquired.getId())
                                    .quantity(acquired.getQuantity())
                                    .unitPrice(acquired.getUnitPrice())
                                    .service(ReservationDTO.ServiceItemSummary.builder()
                                            .id(sId)
                                            .name(serviceName)
                                            .build())
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

        Long clientId = null;
        String clientFirstName = "";
        String clientLastName = "";
        String clientEmail = "";
        String clientPhone = "";

        if (reservation.getClient() != null) {
            clientId = reservation.getClient().getId();
            clientFirstName = reservation.getClient().getFirstName();
            clientLastName = reservation.getClient().getLastName();
            clientEmail = reservation.getClient().getEmail();
            clientPhone = reservation.getClient().getPhone();
        }

        boolean isPaid = reservation.isPaid();
        double pendingBalance = isPaid ? 0.0 : totalPrice;

        ReservationDTO.ClientSummary clientSummary = ReservationDTO.ClientSummary.builder()
                .id(clientId)
                .firstName(clientFirstName)
                .lastName(clientLastName)
                .email(clientEmail)
                .phone(clientPhone)
                .build();

        List<ReservationDTO.ReservationRoomSummary> resRoomSummaries = new ArrayList<>();
        if (reservationRoom != null) {
            ReservationDTO.RoomSummary rSummary = ReservationDTO.RoomSummary.builder()
                    .id(roomId)
                    .number(roomNumber)
                    .name(roomName)
                    .pricePerNight(pricePerNight)
                    .roomType(ReservationDTO.RoomTypeSummary.builder()
                            .id(roomTypeId)
                            .name(roomTypeName)
                            .build())
                    .build();

            resRoomSummaries.add(
                    ReservationDTO.ReservationRoomSummary.builder()
                            .id(reservationRoom.getId())
                            .pricePerNight(pricePerNight)
                            .room(rSummary)
                            .acquiredServices(acquiredSummaries)
                            .build());
        }

        return ReservationDTO.builder()
                .id(reservation.getId())
                .clientId(clientId)
                .clientFirstName(clientFirstName)
                .clientLastName(clientLastName)
                .clientEmail(clientEmail)
                .clientPhone(clientPhone)
                .roomId(roomId)
                .roomNumber(roomNumber)
                .roomName(roomName)
                .roomTypeName(roomTypeName)
                .roomTypeId(roomTypeId)
                .pricePerNight(pricePerNight)
                .checkInDate(reservation.getCheckInDate())
                .checkOutDate(reservation.getCheckOutDate())
                .numberOfPeople(reservation.getNumberOfPeople())
                .status(reservation.getStatus())
                .paid(isPaid)
                .pendingBalance(pendingBalance)
                .nights(nights)
                .roomSubtotal(roomSubtotal)
                .servicesSubtotal(servicesSubtotal)
                .totalPrice(totalPrice)
                .services(services)
                .client(clientSummary)
                .reservationRooms(resRoomSummaries)
                .build();
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
                .paid(false)
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

        if (STATUS_INACTIVE.equalsIgnoreCase(reservation.getStatus())) {
            throw new IllegalArgumentException(
                    "No se puede modificar una reserva cuya estancia ya ha finalizado.");
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

    /**
     * Eliminación de reserva. Por requisito de negocio del historial de reservas,
     * no se elimina físicamente de la base de datos, sino que se cancela lógicamente.
     */
    @Override
    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID de la reserva es obligatorio.");
        }

        Reservation reservation =
                reservationRepository.findById(id).orElse(null);

        if (reservation != null) {
            reservation.setStatus(STATUS_CANCELLED);
            reservationRepository.save(reservation);
        }
    }

    /**
     * Cancela la reserva cambiando su estado a CANCELLED y liberando la habitación para el inventario.
     */
    @Override
    @Transactional
    public ReservationDTO cancelReservation(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID de la reserva es obligatorio.");
        }

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(id));

        if (STATUS_INACTIVE.equalsIgnoreCase(reservation.getStatus())) {
            throw new IllegalArgumentException("No se puede cancelar una estancia que ya ha finalizado.");
        }

        reservation.setStatus(STATUS_CANCELLED);
        Reservation saved = reservationRepository.save(reservation);
        return toDTO(saved);
    }

    /**
     * Activa la estancia del cliente (Check-in / Huésped llega a su reserva).
     * Estado pasa a ACTIVE.
     */
    @Override
    @Transactional
    public ReservationDTO activateStay(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID de la reserva es obligatorio.");
        }

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(id));

        if (STATUS_CANCELLED.equalsIgnoreCase(reservation.getStatus())) {
            throw new IllegalArgumentException("No se puede activar una reserva que ha sido cancelada.");
        }

        if (STATUS_INACTIVE.equalsIgnoreCase(reservation.getStatus())) {
            throw new IllegalArgumentException("No se puede activar una reserva cuya estancia ya ha finalizado.");
        }

        reservation.setStatus(STATUS_ACTIVE);
        Reservation saved = reservationRepository.save(reservation);
        return toDTO(saved);
    }

    /**
     * Finaliza la estancia del cliente (Check-out). Estado pasa a INACTIVE.
     * REGLA ESTRICTA DE NEGOCIO: Solo se permite si la cuenta está vacía / ya pagó (paid == true).
     */
    @Override
    @Transactional
    public ReservationDTO finishStay(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID de la reserva es obligatorio.");
        }

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(id));

        if (STATUS_CANCELLED.equalsIgnoreCase(reservation.getStatus())) {
            throw new IllegalArgumentException("No se puede finalizar una reserva cancelada.");
        }

        // VALIDACIÓN CRÍTICA DE NEGOCIO: Cuenta debe estar en 0 (pagada)
        if (!reservation.isPaid()) {
            throw new IllegalStateException(
                    "No se permite finalizar la estadía del cliente porque la cuenta no ha sido pagada. "
                    + "Debe registrar el pago de la cuenta antes de dar salida al huésped.");
        }

        reservation.setStatus(STATUS_INACTIVE);
        Reservation saved = reservationRepository.save(reservation);
        return toDTO(saved);
    }

    /**
     * Paga la cuenta de la estancia.
     * Limpia la cuenta del cliente (paid = true, saldo pendiente = 0.0).
     */
    @Override
    @Transactional
    public ReservationDTO payStayAccount(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID de la reserva es obligatorio.");
        }

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(id));

        if (STATUS_CANCELLED.equalsIgnoreCase(reservation.getStatus())) {
            throw new IllegalArgumentException("No se puede liquidar la cuenta de una reserva cancelada.");
        }

        // Se limpia la cuenta del cliente
        reservation.setPaid(true);
        Reservation saved = reservationRepository.save(reservation);
        return toDTO(saved);
    }
}
