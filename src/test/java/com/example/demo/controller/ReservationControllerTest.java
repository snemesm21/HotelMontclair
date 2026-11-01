package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.entities.Reservation;
import com.example.demo.service.ReservationManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReservationControllerTest {

    private ReservationController controller;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        controller = new ReservationController();

        Client client = Client.builder()
                .id(1L)
                .username("mockUser")
                .password("password")
                .email("mock@example.com")
                .firstName("Mock")
                .lastName("User")
                .role("CLIENT")
                .build();

        reservation = Reservation.builder()
                .id(10L)
                .client(client)
                .checkInDate(LocalDate.of(2026, 12, 10))
                .checkOutDate(LocalDate.of(2026, 12, 15))
                .numberOfPeople(2)
                .status("CONFIRMED")
                .build();

        ReservationManagerService mockService = new ReservationManagerService() {
            @Override
            public List<Reservation> searchAll() {
                return List.of(reservation);
            }

            @Override
            public Reservation searchById(Long id) {
                return Long.valueOf(10L).equals(id) ? reservation : null;
            }

            @Override
            public List<Reservation> searchByClientId(Long clientId) {
                return Long.valueOf(1L).equals(clientId) ? List.of(reservation) : List.of();
            }

            @Override
            public Reservation save(Reservation reservation) {
                return reservation;
            }

            @Override
            public Reservation createReservation(Long clientId, Long roomTypeId,
                    LocalDate checkInDate, LocalDate checkOutDate,
                    int numberOfPeople, List<Long> serviceIds) {
                return reservation;
            }

            @Override
            public Reservation updateReservation(Long id, Long clientId,
                    Long roomTypeId, LocalDate checkInDate,
                    LocalDate checkOutDate, int numberOfPeople,
                    List<Long> serviceIds) {
                return reservation;
            }

            @Override
            public void delete(Long id) {
            }
        };

        ReflectionTestUtils.setField(controller, "reservationService", mockService);
    }

    @Test
    void getAllReservations_shouldReturnReservations() {
        List<Reservation> result = controller.getAllReservations();

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
    }

    @Test
    void getReservationById_shouldReturnReservation() {
        assertNotNull(controller.getReservationById(10L).getBody());
    }

    @Test
    void getReservationsByClientId_shouldReturnReservations() {
        List<Reservation> result = controller.getReservationsByClientId(1L);

        assertEquals(1, result.size());
    }

    @Test
    void createReservation_shouldReturnCreated() {
        Map<String, Object> request = Map.of(
                "clientId", 1L,
                "roomTypeId", 2L,
                "checkInDate", "2026-12-10",
                "checkOutDate", "2026-12-15",
                "numberOfPeople", 2);

        var response = controller.createReservation(request);

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    void updateReservation_shouldReturnOk() {
        Map<String, Object> request = Map.of(
                "clientId", 1L,
                "roomTypeId", 2L,
                "checkInDate", "2026-12-11",
                "checkOutDate", "2026-12-16",
                "numberOfPeople", 2);

        var response = controller.updateReservation(10L, request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    void deleteReservation_shouldReturnNoContent() {
        var response = controller.deleteReservation(10L);

        assertEquals(204, response.getStatusCode().value());
    }
}
