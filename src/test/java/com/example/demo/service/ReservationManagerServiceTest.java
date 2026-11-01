package com.example.demo.service;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.entities.Reservation;
import com.example.demo.entities.Room;
import com.example.demo.entities.Service;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ReservationManagerServiceTest {

    @Autowired
    private ReservationManagerService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    private Long testReservationId;
    private Long testRoomId;
    private Long testServiceId;

    @BeforeEach
    void setUp() {
        Reservation reservation = reservationRepository.findAll().stream().findFirst().orElse(null);
        assertNotNull(reservation, "Debe existir al menos una reserva inicial");
        testReservationId = reservation.getId();
        reservation.setStatus("CONFIRMED");
        reservation.setPaid(false);
        // Fecha futura para cumplir la Regla 4 por defecto
        reservation.setCheckInDate(LocalDate.now().plusDays(5));
        reservation.setCheckOutDate(LocalDate.now().plusDays(8));
        reservationRepository.save(reservation);

        if (reservation.getReservationRooms() != null && !reservation.getReservationRooms().isEmpty()) {
            testRoomId = reservation.getReservationRooms().get(0).getRoom().getId();
        } else {
            Room room = roomRepository.findAll().stream().findFirst().orElseThrow();
            testRoomId = room.getId();
        }

        Service service = serviceRepository.findAll().stream().filter(s -> !s.isHidden()).findFirst().orElseThrow();
        testServiceId = service.getId();
    }

    @Test
    @DisplayName("Debe activar la estadia cambiando el estado a ACTIVE")
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
    @DisplayName("No debe permitir finalizar la estadia si la cuenta no ha sido pagada")
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
    @DisplayName("Debe permitir finalizar la estadia si la cuenta ya esta pagada")
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
    @DisplayName("Regla 4: Debe cancelar la reserva si no esta activa y su fecha es futura")
    void testCancelReservationPreservesHistory() {
        Reservation res = reservationRepository.findById(testReservationId).orElseThrow();
        res.setStatus("CONFIRMED");
        res.setCheckInDate(LocalDate.now().plusDays(4));
        reservationRepository.save(res);

        ReservationDTO dto = reservationService.cancelReservation(testReservationId);
        assertNotNull(dto);
        assertEquals("CANCELLED", dto.getStatus());

        // Verificar que sigue existiendo en el repositorio (cancelacion logica)
        assertTrue(reservationRepository.existsById(testReservationId));
    }

    @Test
    @DisplayName("Regla 4: No debe permitir cancelar una reserva que ya se encuentra activa")
    void testCannotCancelActiveReservationThrowsException() {
        Reservation res = reservationRepository.findById(testReservationId).orElseThrow();
        res.setStatus("ACTIVE");
        reservationRepository.save(res);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            reservationService.cancelReservation(testReservationId);
        });

        assertTrue(ex.getMessage().contains("todavia no estan activas"));
    }

    @Test
    @DisplayName("Regla 4: No debe permitir cancelar una reserva si su fecha de inicio no es posterior a la actual")
    void testCannotCancelReservationWhenCheckInDateIsNotAfterToday() {
        Reservation res = reservationRepository.findById(testReservationId).orElseThrow();
        res.setStatus("CONFIRMED");
        // Fecha de hoy (no posterior)
        res.setCheckInDate(LocalDate.now());
        reservationRepository.save(res);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            reservationService.cancelReservation(testReservationId);
        });

        assertTrue(ex.getMessage().contains("posterior a la fecha actual"));
    }

    @Test
    @DisplayName("Regla 5: En caso de que en la habitacion no haya ninguna reserva activa, no se debe poder asignar ningun servicio")
    void testCannotAssignServiceToRoomWithoutActiveReservation() {
        // La reserva esta CONFIRMED, no ACTIVE
        Reservation res = reservationRepository.findById(testReservationId).orElseThrow();
        res.setStatus("CONFIRMED");
        reservationRepository.save(res);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            reservationService.assignServiceToRoom(testRoomId, testServiceId, 1);
        });

        assertTrue(ex.getMessage().contains("no haya ninguna reserva activa"));
    }

    @Test
    @DisplayName("Regla 5: Si en la habitacion hay una reserva activa, debe permitir asignar el servicio y marcar cuenta no pagada")
    void testAssignServiceToRoomWithActiveReservationSuccess() {
        // Ponemos la reserva en ACTIVE
        Reservation res = reservationRepository.findById(testReservationId).orElseThrow();
        res.setStatus("ACTIVE");
        res.setPaid(true); // Estaba pagada previamente
        reservationRepository.save(res);

        ReservationDTO dto = reservationService.assignServiceToRoom(testRoomId, testServiceId, 2);
        assertNotNull(dto);
        assertEquals("ACTIVE", dto.getStatus());
        // Al agregar consumo, la cuenta queda pendiente de pago
        assertFalse(dto.isPaid());
        assertTrue(dto.getPendingBalance() > 0);
    }
}