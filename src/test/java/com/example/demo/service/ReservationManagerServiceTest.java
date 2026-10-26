package com.example.demo.service;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.entities.Reservation;
import com.example.demo.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ReservationManagerServiceTest {

    @Autowired
    private ReservationManagerService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    private Long testReservationId;

    @BeforeEach
    void setUp() {
        Reservation reservation = reservationRepository.findAll().stream().findFirst().orElse(null);
        assertNotNull(reservation, "Debe existir al menos una reserva inicial");
        testReservationId = reservation.getId();
        reservation.setStatus("CONFIRMED");
        reservation.setPaid(false);
        reservationRepository.save(reservation);
    }

    @Test
    @DisplayName("Debe activar la estadía cambiando el estado a ACTIVE")
    void testActivateStay() {
        ReservationDTO dto = reservationService.activateStay(testReservationId);
        assertNotNull(dto);
        assertEquals("ACTIVE", dto.getStatus());
    }

    @Test
    @DisplayName("Debe pagar la cuenta dejando paid en true y saldo pendiente en 0")
    void testPayStayAccount() {
        ReservationDTO dto = reservationService.payStayAccount(testReservationId);
        assertNotNull(dto);
        assertTrue(dto.isPaid());
        assertEquals(0.0, dto.getPendingBalance());
    }

    @Test
    @DisplayName("No debe permitir finalizar la estadía si la cuenta no ha sido pagada")
    void testFinishStayWithoutPaymentThrowsException() {
        Reservation res = reservationRepository.findById(testReservationId).orElseThrow();
        res.setPaid(false);
        res.setStatus("ACTIVE");
        reservationRepository.save(res);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            reservationService.finishStay(testReservationId);
        });

        assertTrue(ex.getMessage().contains("cuenta no ha sido pagada"));
    }

    @Test
    @DisplayName("Debe permitir finalizar la estadía si la cuenta ya está pagada")
    void testFinishStayWithPaymentSuccess() {
        Reservation res = reservationRepository.findById(testReservationId).orElseThrow();
        res.setPaid(true);
        res.setStatus("ACTIVE");
        reservationRepository.save(res);

        ReservationDTO dto = reservationService.finishStay(testReservationId);
        assertNotNull(dto);
        assertEquals("INACTIVE", dto.getStatus());
    }

    @Test
    @DisplayName("Debe cancelar la reserva en lugar de eliminarla físicamente")
    void testCancelReservationPreservesHistory() {
        Reservation res = reservationRepository.findById(testReservationId).orElseThrow();
        res.setStatus("CONFIRMED");
        reservationRepository.save(res);

        ReservationDTO dto = reservationService.cancelReservation(testReservationId);
        assertNotNull(dto);
        assertEquals("CANCELLED", dto.getStatus());

        // Verificar que sigue existiendo en el repositorio
        assertTrue(reservationRepository.existsById(testReservationId));
    }
}