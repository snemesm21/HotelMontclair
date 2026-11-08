
package com.example.demo.service;

import com.example.demo.entities.Client;
import com.example.demo.entities.Reservation;
import com.example.demo.entities.Room;
import com.example.demo.entities.RoomStatus;
import com.example.demo.entities.RoomType;

import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.RoomTypeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReservationManagerServiceIntegrationTest {

    @Autowired
    private ReservationManagerService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private RoomRepository roomRepository;

    private Client client;
    private RoomType roomType;

    @BeforeEach
    void setUp() {

        client = clientRepository.save(Client.builder()
                .username("integrationUser")
                .password("password")
                .email("integration@example.com")
                .firstName("Integration")
                .lastName("Test")
                .phone("123456789")
                .role("CLIENT")
                .build());

        roomType = roomTypeRepository.save(RoomType.builder()
                .name("Integration Deluxe")
                .description("Room for integration test")
                .pricePerNight(250.0)
                .build());

        roomRepository.save(Room.builder()
                .number("INT-101")
                .floor(1)
                .roomType(roomType)
                .status(RoomStatus.AVAILABLE)
                .capacity(2)
                .pricePerNight(250.0)
                .build());
    }

    // PRUEBA 1: CREAR RESERVA SIN MOCKS

    @Test
    void createReservation_shouldSaveReservationWithNoMocks() {

        // Arrange
        LocalDate checkIn = LocalDate.now().plusDays(30);
        LocalDate checkOut = checkIn.plusDays(3);

        // Act
        Reservation result = reservationService.createReservation(
                client.getId(),
                roomType.getId(),
                checkIn,
                checkOut,
                2,
                null);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getId());

        assertEquals(
                client.getId(),
                result.getClient().getId());

        assertEquals("CONFIRMED", result.getStatus());

        assertEquals(checkIn, result.getCheckInDate());
        assertEquals(checkOut, result.getCheckOutDate());

        List<Reservation> reservations =
                reservationRepository.findByClientId(
                        client.getId());

        assertEquals(1, reservations.size());
        assertEquals(result.getId(), reservations.get(0).getId());
    }

    // PRUEBA 2: BUSCAR RESERVA SIN MOCKS

    @Test
    void searchById_shouldFindSavedReservationWithNoMocks() {

        // Arrange
        LocalDate checkIn = LocalDate.now().plusDays(40);
        LocalDate checkOut = checkIn.plusDays(2);

        Reservation saved = reservationService.createReservation(
                client.getId(),
                roomType.getId(),
                checkIn,
                checkOut,
                2,
                null);

        // Act
        Reservation result =
                reservationService.searchById(saved.getId());

        // Assert
        assertNotNull(result);

        assertEquals(saved.getId(), result.getId());

        assertEquals(
                client.getId(),
                result.getClient().getId());

        assertEquals(checkIn, result.getCheckInDate());
        assertEquals(checkOut, result.getCheckOutDate());
    }

    // PRUEBA 3: ACTUALIZAR RESERVA SIN MOCKS

    @Test
    void updateReservation_shouldUpdateSavedReservationWithNoMocks() {

        // Arrange
        LocalDate originalCheckIn =
                LocalDate.now().plusDays(50);

        LocalDate originalCheckOut =
                originalCheckIn.plusDays(2);

        Reservation saved = reservationService.createReservation(
                client.getId(),
                roomType.getId(),
                originalCheckIn,
                originalCheckOut,
                2,
                null);

        LocalDate newCheckIn =
                originalCheckIn.plusDays(1);

        LocalDate newCheckOut =
                originalCheckIn.plusDays(4);

        // Act
        Reservation updated = reservationService.updateReservation(
                saved.getId(),
                client.getId(),
                roomType.getId(),
                newCheckIn,
                newCheckOut,
                2,
                null);

        // Assert
        assertNotNull(updated);

        assertEquals(saved.getId(), updated.getId());
        assertEquals(newCheckIn, updated.getCheckInDate());
        assertEquals(newCheckOut, updated.getCheckOutDate());

        Reservation result =
                reservationRepository.findById(saved.getId())
                        .orElse(null);

        assertNotNull(result);
        assertEquals(newCheckIn, result.getCheckInDate());
        assertEquals(newCheckOut, result.getCheckOutDate());
    }

    // PRUEBA 4: CANCELAR RESERVA SIN MOCKS

    @Test
    void delete_shouldCancelSavedReservationWithNoMocks() {

        // Arrange
        LocalDate checkIn =
                LocalDate.now().plusDays(60);

        LocalDate checkOut =
                checkIn.plusDays(2);

        Reservation saved = reservationService.createReservation(
                client.getId(),
                roomType.getId(),
                checkIn,
                checkOut,
                2,
                null);

        Long reservationId = saved.getId();

        // Act
        reservationService.delete(reservationId);

        // Assert
        Reservation result =
                reservationService.searchById(reservationId);

        assertNotNull(result);
        assertEquals("CANCELLED", result.getStatus());

        assertTrue(
                reservationRepository.findById(reservationId)
                        .isPresent());
    }
}
