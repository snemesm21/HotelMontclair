package com.example.demo.repository;

import com.example.demo.entities.Client;
import com.example.demo.entities.Reservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ReservationRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ClientRepository clientRepository;

    private Client client;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        clientRepository.deleteAll();

        client = clientRepository.save(Client.builder()
                .username("repositoryUser")
                .password("password")
                .email("repository@example.com")
                .firstName("Repository")
                .lastName("Test")
                .phone("123456789")
                .role("CLIENT")
                .build());

        reservation = reservationRepository.save(Reservation.builder()
                .client(client)
                .checkInDate(LocalDate.of(2026, 12, 10))
                .checkOutDate(LocalDate.of(2026, 12, 15))
                .numberOfPeople(2)
                .status("CONFIRMED")
                .build());
    }

    @Test
    void reservationRepository_save_shouldReturnReservation() {
        assertNotNull(reservation.getId());
        assertEquals("CONFIRMED", reservation.getStatus());
    }

    @Test
    void reservationRepository_findAll_shouldReturnReservation() {
        List<Reservation> reservations = reservationRepository.findAll();
        assertEquals(1, reservations.size());
        assertEquals(reservation.getId(), reservations.get(0).getId());
    }

    @Test
    void reservationRepository_findById_shouldReturnReservation() {
        Reservation result = reservationRepository.findById(reservation.getId()).orElse(null);
        assertNotNull(result);
        assertEquals(reservation.getId(), result.getId());
    }

    @Test
    void reservationRepository_update_shouldReturnUpdatedReservation() {
        reservation.setStatus("CANCELLED");
        Reservation updated = reservationRepository.save(reservation);

        assertEquals("CANCELLED", updated.getStatus());
    }

    @Test
    void reservationRepository_delete_shouldRemoveReservation() {
        reservationRepository.deleteById(reservation.getId());
        assertTrue(reservationRepository.findById(reservation.getId()).isEmpty());
    }

    @Test
    void reservationRepository_findByClientId_shouldReturnReservation() {
        assertEquals(1, reservationRepository.findByClientId(client.getId()).size());
    }

    @Test
    void reservationRepository_findByStatus_shouldReturnReservation() {
        assertEquals(1, reservationRepository.findByStatus("CONFIRMED").size());
    }

    @Test
    void reservationRepository_findByCheckInDateBetween_shouldReturnReservation() {
        assertEquals(1, reservationRepository.findByCheckInDateBetween(
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31)).size());
    }

    @Test
    void reservationRepository_findByCheckOutDateBetween_shouldReturnReservation() {
        assertEquals(1, reservationRepository.findByCheckOutDateBetween(
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31)).size());
    }

    @Test
    void reservationRepository_findByClientIdAndStatus_shouldReturnReservation() {
        assertEquals(1, reservationRepository.findByClientIdAndStatus(
                client.getId(), "CONFIRMED").size());
    }
}
