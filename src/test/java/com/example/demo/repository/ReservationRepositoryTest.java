
package com.example.demo.repository;

import com.example.demo.entities.Client;
import com.example.demo.entities.Reservation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ReservationRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Client client;
    private Reservation reservation;

    @BeforeEach
    void setUp() {

        client = clientRepository.save(Client.builder()
                .username("repositoryUser")
                .password("password")
                .email("repository@example.com")
                .firstName("Repository")
                .lastName("Test")
                .phone("123456789")
                .role("CLIENT")
                .build());

        reservation = reservationRepository.save(
                Reservation.builder()
                        .client(client)
                        .checkInDate(LocalDate.of(2026, 12, 10))
                        .checkOutDate(LocalDate.of(2026, 12, 15))
                        .numberOfPeople(2)
                        .status("CONFIRMED")
                        .build());

        entityManager.flush();
        entityManager.clear();
    }

    // PRUEBA 1: CREATE

    @Test
    void reservationRepository_save_shouldReturnReservation() {

        // Arrange
        Reservation newReservation = Reservation.builder()
                .client(client)
                .checkInDate(LocalDate.of(2027, 1, 10))
                .checkOutDate(LocalDate.of(2027, 1, 15))
                .numberOfPeople(3)
                .status("CONFIRMED")
                .build();

        // Act
        Reservation saved =
                reservationRepository.save(newReservation);

        entityManager.flush();
        entityManager.clear();

        Reservation result = reservationRepository
                .findById(saved.getId())
                .orElse(null);

        // Assert
        assertNotNull(saved.getId());
        assertNotNull(result);
        assertEquals("CONFIRMED", result.getStatus());
        assertEquals(3, result.getNumberOfPeople());
        assertEquals(client.getId(), result.getClient().getId());
    }

    // PRUEBA 2: READ ALL

    @Test
    void reservationRepository_findAll_shouldReturnReservation() {

        // Arrange
        Long expectedId = reservation.getId();

        // Act
        List<Reservation> reservations =
                reservationRepository.findAll();

        // Assert
        assertNotNull(reservations);
        assertEquals(1, reservations.size());
        assertEquals(expectedId, reservations.get(0).getId());
    }

    // PRUEBA 3: READ BY ID

    @Test
    void reservationRepository_findById_shouldReturnReservation() {

        // Arrange
        Long id = reservation.getId();

        // Act
        Optional<Reservation> result =
                reservationRepository.findById(id);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
        assertEquals("CONFIRMED", result.get().getStatus());
    }

    // PRUEBA 4: UPDATE

    @Test
    void reservationRepository_update_shouldReturnUpdatedReservation() {

        // Arrange
        Reservation existing = reservationRepository
                .findById(reservation.getId())
                .orElseThrow();

        existing.setStatus("CANCELLED");

        // Act
        reservationRepository.save(existing);

        entityManager.flush();
        entityManager.clear();

        Reservation updated = reservationRepository
                .findById(reservation.getId())
                .orElse(null);

        // Assert
        assertNotNull(updated);
        assertEquals("CANCELLED", updated.getStatus());
    }

    // PRUEBA 5: DELETE

    @Test
    void reservationRepository_delete_shouldRemoveReservation() {

        // Arrange
        Long id = reservation.getId();

        // Act
        reservationRepository.deleteById(id);

        entityManager.flush();
        entityManager.clear();

        Optional<Reservation> result =
                reservationRepository.findById(id);

        // Assert
        assertTrue(result.isEmpty());
    }

    // PRUEBA 6: QUERY POR CLIENTE

    @Test
    void reservationRepository_findByClientId_shouldReturnReservation() {

        // Arrange
        Long clientId = client.getId();

        // Act
        List<Reservation> result =
                reservationRepository.findByClientId(clientId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(clientId, result.get(0).getClient().getId());
        assertEquals(reservation.getId(), result.get(0).getId());
    }

    // PRUEBA 7: QUERY POR ESTADO

    @Test
    void reservationRepository_findByStatus_shouldReturnReservation() {

        // Arrange
        String status = "CONFIRMED";

        // Act
        List<Reservation> result =
                reservationRepository.findByStatus(status);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(status, result.get(0).getStatus());
    }

    // PRUEBA 8: QUERY POR FECHA DE ENTRADA

    @Test
    void reservationRepository_findByCheckInDateBetween_shouldReturnReservation() {

        // Arrange
        LocalDate start = LocalDate.of(2026, 12, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);

        // Act
        List<Reservation> result =
                reservationRepository.findByCheckInDateBetween(
                        start, end);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        LocalDate checkIn = result.get(0).getCheckInDate();

        assertFalse(checkIn.isBefore(start));
        assertFalse(checkIn.isAfter(end));
    }

    // PRUEBA 9: QUERY POR FECHA DE SALIDA

    @Test
    void reservationRepository_findByCheckOutDateBetween_shouldReturnReservation() {

        // Arrange
        LocalDate start = LocalDate.of(2026, 12, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);

        // Act
        List<Reservation> result =
                reservationRepository.findByCheckOutDateBetween(
                        start, end);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        LocalDate checkOut = result.get(0).getCheckOutDate();

        assertFalse(checkOut.isBefore(start));
        assertFalse(checkOut.isAfter(end));
    }

    // PRUEBA 10: QUERY POR CLIENTE Y ESTADO

    @Test
    void reservationRepository_findByClientIdAndStatus_shouldReturnReservation() {

        // Arrange
        Long clientId = client.getId();
        String status = "CONFIRMED";

        // Act
        List<Reservation> result =
                reservationRepository.findByClientIdAndStatus(
                        clientId, status);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(clientId, result.get(0).getClient().getId());
        assertEquals(status, result.get(0).getStatus());
    }
}
