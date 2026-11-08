
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
import com.example.demo.repository.RoomTypeRepository;
import com.example.demo.repository.ServiceRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationManagerServiceMockTest {

    @InjectMocks
    private ReservationManagerServiceImpl service;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @Mock
    private ReservationRoomRepository reservationRoomRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ServiceRepository serviceRepository;

    private Client client;
    private Reservation reservation;

    @BeforeEach
    void setUp() {

        client = Client.builder()
                .id(1L)
                .firstName("Mock")
                .lastName("Client")
                .email("mock@example.com")
                .build();

        reservation = Reservation.builder()
                .id(10L)
                .client(client)
                .checkInDate(LocalDate.now().plusDays(10))
                .checkOutDate(LocalDate.now().plusDays(15))
                .numberOfPeople(2)
                .status("CONFIRMED")
                .build();
    }

    // PRUEBA 1: CONSULTAR TODAS LAS RESERVAS

    @Test
    void reservationService_searchAll_shouldReturnReservations() {

        // Arrange
        when(reservationRepository.findAll())
                .thenReturn(List.of(reservation));

        // Act
        List<Reservation> result = service.searchAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());

        verify(reservationRepository).findAll();
    }

    // PRUEBA 2: CONSULTAR RESERVA POR ID

    @Test
    void reservationService_searchById_shouldReturnReservation() {

        // Arrange
        when(reservationRepository.findById(10L))
                .thenReturn(Optional.of(reservation));

        // Act
        Reservation result = service.searchById(10L);

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("CONFIRMED", result.getStatus());

        verify(reservationRepository).findById(10L);
    }

    // PRUEBA 3: CONSULTAR RESERVAS POR CLIENTE

    @Test
    void reservationService_searchByClientId_shouldReturnReservations() {

        // Arrange
        when(reservationRepository.findByClientId(1L))
                .thenReturn(List.of(reservation));

        // Act
        List<Reservation> result =
                service.searchByClientId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getClient().getId());

        verify(reservationRepository).findByClientId(1L);
    }

    // PRUEBA 4: CREAR RESERVA CON MOCKS

    @Test
    void reservationService_createReservation_shouldReturnReservation() {

        // Arrange
        RoomType roomType = RoomType.builder()
                .id(2L)
                .name("Deluxe")
                .build();

        Room room = Room.builder()
                .id(20L)
                .number("101")
                .roomType(roomType)
                .status(RoomStatus.AVAILABLE)
                .capacity(2)
                .pricePerNight(100.0)
                .build();

        LocalDate checkIn = LocalDate.now().plusDays(30);
        LocalDate checkOut = checkIn.plusDays(3);

        when(clientRepository.findById(1L))
                .thenReturn(Optional.of(client));

        // Verificar que existe el tipo de habitacion
        when(roomTypeRepository.existsById(2L))
                .thenReturn(true);

        // Simular habitaciones disponibles
        when(roomRepository
                .findByRoomType_IdAndStatusAndCapacityGreaterThanEqualOrderByIdAsc(
                        2L,
                        RoomStatus.AVAILABLE,
                        2))
                .thenReturn(List.of(room));

        // Simular que no hay habitaciones ocupadas
        when(reservationRoomRepository.findOccupiedRoomIds(
                any(), any(), any()))
                .thenReturn(List.of());

        // Simular el guardado de la reserva
        when(reservationRepository.save(
                any(Reservation.class)))
                .thenAnswer(invocation -> {

                    Reservation saved =
                            invocation.getArgument(0);

                    saved.setId(10L);

                    return saved;
                });

        // Act
        Reservation result = service.createReservation(
                1L,
                2L,
                checkIn,
                checkOut,
                2,
                null);

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("CONFIRMED", result.getStatus());
        assertEquals(checkIn, result.getCheckInDate());
        assertEquals(checkOut, result.getCheckOutDate());
        assertEquals(2, result.getNumberOfPeople());

        verify(clientRepository).findById(1L);
        verify(roomTypeRepository).existsById(2L);

        verify(roomRepository)
                .findByRoomType_IdAndStatusAndCapacityGreaterThanEqualOrderByIdAsc(
                        2L,
                        RoomStatus.AVAILABLE,
                        2);

        verify(reservationRepository)
                .save(any(Reservation.class));
    }

    // PRUEBA 5: CANCELAR RESERVA CON MOCKS

    @Test
    void reservationService_delete_shouldCancelReservation() {

        // Arrange
        reservation.setCheckInDate(
                LocalDate.now().plusDays(30));

        when(reservationRepository.findById(10L))
                .thenReturn(Optional.of(reservation));

        when(reservationRepository.save(
                any(Reservation.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        // Act
        service.delete(10L);

        // Assert
        assertEquals("CANCELLED", reservation.getStatus());

        verify(reservationRepository).findById(10L);

        verify(reservationRepository)
                .save(reservation);

        verify(reservationRepository, never())
                .delete(any(Reservation.class));
    }
}
