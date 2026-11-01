package com.example.demo.controller;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.dto.ReservationRequestDTO;
import com.example.demo.entities.Client;
import com.example.demo.entities.Reservation;
import com.example.demo.service.ReservationManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationControllerTest {

    private ReservationController controller;
    private Reservation reservation;
    private ReservationDTO summary;
    private ReservationManagerService mockService;

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

        summary = ReservationDTO.builder()
                .id(10L)
                .clientId(1L)
                .clientFirstName("Mock")
                .clientLastName("User")
                .checkInDate(LocalDate.of(2026, 12, 10))
                .checkOutDate(LocalDate.of(2026, 12, 15))
                .numberOfPeople(2)
                .status("CONFIRMED")
                .build();

        mockService = mock(ReservationManagerService.class);
        when(mockService.getReservationSummaries()).thenReturn(List.of(summary));
        when(mockService.findSummaryById(10L)).thenReturn(summary);
        when(mockService.findSummaryById(99L)).thenReturn(null);
        when(mockService.getSummariesByClientId(1L)).thenReturn(List.of(summary));
        when(mockService.getSummariesByClientId(99L)).thenReturn(List.of());
        when(mockService.createReservation(eq(1L), eq(2L), any(LocalDate.class),
                any(LocalDate.class), eq(2), any())).thenReturn(reservation);
        when(mockService.updateReservation(eq(10L), eq(1L), eq(2L), any(LocalDate.class),
                any(LocalDate.class), eq(2), any())).thenReturn(reservation);

        ReflectionTestUtils.setField(controller, "reservationService", mockService);
    }

    @Test
    void getAllReservations_shouldReturnReservations() {
        List<ReservationDTO> result = controller.getAllReservations();

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
    }

    @Test
    void getReservationById_shouldReturnReservation() {
        assertNotNull(controller.getReservationById(10L).getBody());
    }

    @Test
    void getReservationsByClientId_shouldReturnReservations() {
        List<ReservationDTO> result = controller.getReservationsByClientId(1L);

        assertEquals(1, result.size());
    }

    @Test
    void createReservation_shouldReturnCreated() {
        ReservationRequestDTO request = new ReservationRequestDTO(
                1L, 2L,
                LocalDate.of(2026, 12, 10),
                LocalDate.of(2026, 12, 15), 2, null);

        var response = controller.createReservation(request);

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    void updateReservation_shouldReturnOk() {
        ReservationRequestDTO request = new ReservationRequestDTO(
                1L, 2L,
                LocalDate.of(2026, 12, 11),
                LocalDate.of(2026, 12, 16), 2, null);

        var response = controller.updateReservation(10L, request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }

    @Test
    void deleteReservation_shouldReturnNoContent() {
        var response = controller.deleteReservation(10L);

        assertEquals(204, response.getStatusCode().value());
        verify(mockService).delete(10L);
    }
}
