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

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
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
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        roomTypeRepository.deleteAll();
        clientRepository.deleteAll();

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

    @Test
    void createReservation_shouldSaveReservationWithNoMocks() {
        Reservation result = reservationService.createReservation(
                client.getId(),
                roomType.getId(),
                LocalDate.of(2026, 12, 20),
                LocalDate.of(2026, 12, 23),
                2,
                null);

        assertNotNull(result.getId());
        assertEquals(client.getId(), result.getClient().getId());
        assertEquals("CONFIRMED", result.getStatus());
        assertEquals(1, reservationRepository.findByClientId(client.getId()).size());
    }

    @Test
    void searchById_shouldFindSavedReservationWithNoMocks() {
        Reservation saved = reservationService.createReservation(
                client.getId(), roomType.getId(),
                LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 12), 2, null);

        Reservation result = reservationService.searchById(saved.getId());

        assertNotNull(result);
        assertEquals(saved.getId(), result.getId());
    }

    @Test
    void updateReservation_shouldUpdateSavedReservationWithNoMocks() {
        Reservation saved = reservationService.createReservation(
                client.getId(), roomType.getId(),
                LocalDate.of(2027, 2, 10), LocalDate.of(2027, 2, 12), 2, null);

        Reservation updated = reservationService.updateReservation(
                saved.getId(), client.getId(), roomType.getId(),
                LocalDate.of(2027, 2, 11), LocalDate.of(2027, 2, 14), 2, null);

        assertNotNull(updated);
        assertEquals(LocalDate.of(2027, 2, 11), updated.getCheckInDate());
        assertEquals(LocalDate.of(2027, 2, 14), updated.getCheckOutDate());
    }

    @Test
    void delete_shouldRemoveSavedReservationWithNoMocks() {
        Reservation saved = reservationService.createReservation(
                client.getId(), roomType.getId(),
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, null);

        reservationService.delete(saved.getId());

        assertNull(reservationService.searchById(saved.getId()));
    }
}
