
package com.example.demo.controller;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.entities.Client;
import com.example.demo.entities.Reservation;
import com.example.demo.service.ReservationManagerService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservationController.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationManagerService reservationService;

    private Reservation reservation;
    private ReservationDTO summary;

    @BeforeEach
    void setUp() {

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
    }

    // PRUEBA 1: GET TODAS LAS RESERVAS

    @Test
    void reservationController_getAll_shouldReturnReservations()
            throws Exception {

        // Arrange
        when(reservationService.getReservationSummaries())
                .thenReturn(List.of(summary));

        // Act y Assert
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].clientId").value(1))
                .andExpect(jsonPath("$[0].status")
                        .value("CONFIRMED"));

        verify(reservationService)
                .getReservationSummaries();
    }

    // PRUEBA 2: GET RESERVA POR ID

    @Test
    void reservationController_getById_shouldReturnReservation()
            throws Exception {

        // Arrange
        when(reservationService.findSummaryById(10L))
                .thenReturn(summary);

        // Act y Assert
        mockMvc.perform(get("/api/reservations/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.clientId").value(1))
                .andExpect(jsonPath("$.status")
                        .value("CONFIRMED"));

        verify(reservationService)
                .findSummaryById(10L);
    }

    // PRUEBA 3: GET RESERVAS POR CLIENTE

    @Test
    void reservationController_getByClientId_shouldReturnReservations()
            throws Exception {

        // Arrange
        when(reservationService.getSummariesByClientId(1L))
                .thenReturn(List.of(summary));

        // Act y Assert
        mockMvc.perform(
                        get("/api/reservations/client/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].clientId").value(1));

        verify(reservationService)
                .getSummariesByClientId(1L);
    }

    // PRUEBA 4: POST CREAR RESERVA

    @Test
    void reservationController_create_shouldReturnCreated()
            throws Exception {

        // Arrange
        String request = """
                {
                    "clientId": 1,
                    "roomTypeId": 2,
                    "checkInDate": "2026-12-10",
                    "checkOutDate": "2026-12-15",
                    "numberOfPeople": 2,
                    "serviceIds": null
                }
                """;

        when(reservationService.createReservation(
                eq(1L),
                eq(2L),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(2),
                isNull()))
                .thenReturn(reservation);

        when(reservationService.findSummaryById(10L))
                .thenReturn(summary);

        // Act y Assert
        mockMvc.perform(
                        post("/api/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status")
                        .value("CONFIRMED"));

        verify(reservationService).createReservation(
                eq(1L),
                eq(2L),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(2),
                isNull());
    }

    // PRUEBA 5: PUT ACTUALIZAR RESERVA

    @Test
    void reservationController_update_shouldReturnOk()
            throws Exception {

        // Arrange
        String request = """
                {
                    "clientId": 1,
                    "roomTypeId": 2,
                    "checkInDate": "2026-12-11",
                    "checkOutDate": "2026-12-16",
                    "numberOfPeople": 2,
                    "serviceIds": null
                }
                """;

        Reservation updated = Reservation.builder()
                .id(10L)
                .status("CONFIRMED")
                .checkInDate(LocalDate.of(2026, 12, 11))
                .checkOutDate(LocalDate.of(2026, 12, 16))
                .numberOfPeople(2)
                .build();

        ReservationDTO updatedSummary = ReservationDTO.builder()
                .id(10L)
                .clientId(1L)
                .checkInDate(LocalDate.of(2026, 12, 11))
                .checkOutDate(LocalDate.of(2026, 12, 16))
                .numberOfPeople(2)
                .status("CONFIRMED")
                .build();

        when(reservationService.updateReservation(
                eq(10L),
                eq(1L),
                eq(2L),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(2),
                isNull()))
                .thenReturn(updated);

        when(reservationService.findSummaryById(10L))
                .thenReturn(updatedSummary);

        // Act y Assert
        mockMvc.perform(
                        put("/api/reservations/10")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.checkInDate")
                        .value("2026-12-11"))
                .andExpect(jsonPath("$.checkOutDate")
                        .value("2026-12-16"));

        verify(reservationService).updateReservation(
                eq(10L),
                eq(1L),
                eq(2L),
                any(LocalDate.class),
                any(LocalDate.class),
                eq(2),
                isNull());
    }

    // PRUEBA 6: DELETE CANCELAR RESERVA

    @Test
    void reservationController_delete_shouldReturnNoContent()
            throws Exception {

        // Arrange
        doNothing().when(reservationService).delete(10L);

        // Act y Assert
        mockMvc.perform(
                        delete("/api/reservations/10"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(reservationService).delete(10L);
    }
}
