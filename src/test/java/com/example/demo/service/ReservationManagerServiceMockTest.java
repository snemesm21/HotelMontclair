package com.example.demo.service;

import com.example.demo.entities.Client;
import com.example.demo.entities.Reservation;
import com.example.demo.entities.Room;
import com.example.demo.entities.RoomStatus;
import com.example.demo.entities.RoomType;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.ReservationRoomRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationManagerServiceMockTest {

    private ReservationManagerServiceImpl service;
    private ReservationRepository reservationRepository;
    private RoomRepository roomRepository;
    private ReservationRoomRepository reservationRoomRepository;
    private ClientRepository clientRepository;
    private ServiceRepository serviceRepository;

    private Client client;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        service = new ReservationManagerServiceImpl();
        reservationRepository = mock(ReservationRepository.class);
        roomRepository = mock(RoomRepository.class);
        reservationRoomRepository = mock(ReservationRoomRepository.class);
        clientRepository = mock(ClientRepository.class);
        serviceRepository = mock(ServiceRepository.class);

        ReflectionTestUtils.setField(service, "reservationRepository", reservationRepository);
        ReflectionTestUtils.setField(service, "roomRepository", roomRepository);
        ReflectionTestUtils.setField(service, "reservationRoomRepository", reservationRoomRepository);
        ReflectionTestUtils.setField(service, "clientRepository", clientRepository);
        ReflectionTestUtils.setField(service, "serviceRepository", serviceRepository);

        client = Client.builder().id(1L).firstName("Mock").lastName("Client").email("mock@example.com").build();
        reservation = Reservation.builder()
                .id(10L)
                .client(client)
                .checkInDate(LocalDate.of(2026, 12, 10))
                .checkOutDate(LocalDate.of(2026, 12, 15))
                .numberOfPeople(2)
                .status("CONFIRMED")
                .build();
    }

    @Test
    void searchAll_shouldReturnMockedReservations() {
        when(reservationRepository.findAll()).thenReturn(List.of(reservation));

        assertEquals(1, service.searchAll().size());
    }

    @Test
    void searchById_shouldReturnMockedReservation() {
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));

        assertNotNull(service.searchById(10L));
    }

    @Test
    void searchByClientId_shouldReturnMockedReservations() {
        when(reservationRepository.findByClientId(1L)).thenReturn(List.of(reservation));

        assertEquals(1, service.searchByClientId(1L).size());
    }

    @Test
    void createReservation_shouldReturnMockedReservation() {
        RoomType roomType = RoomType.builder().id(2L).name("Deluxe").build();
        Room room = Room.builder()
                .id(20L)
                .number("101")
                .roomType(roomType)
                .status(RoomStatus.AVAILABLE)
                .capacity(2)
                .pricePerNight(100.0)
                .build();

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(roomRepository.findByRoomType_IdAndStatusAndCapacityGreaterThanEqualOrderByIdAsc(
                2L, RoomStatus.AVAILABLE, 2)).thenReturn(List.of(room));
        when(reservationRoomRepository.findOccupiedRoomIds(any(), any(), any()))
                .thenReturn(List.of());
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        Reservation result = service.createReservation(
                1L, 2L,
                LocalDate.of(2026, 12, 20),
                LocalDate.of(2026, 12, 23), 2, null);

        assertNotNull(result);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void delete_shouldDeleteMockedReservation() {
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));

        service.delete(10L);

        verify(reservationRepository).delete(reservation);
    }
}
