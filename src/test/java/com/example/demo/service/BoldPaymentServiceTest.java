package com.example.demo.service;

import com.example.demo.dto.BoldCheckoutDTO;
import com.example.demo.dto.ReservationDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BoldPaymentServiceTest {

    private ReservationManagerService reservationService;
    private BoldPaymentService boldPaymentService;

    @BeforeEach
    void setUp() {
        reservationService = mock(ReservationManagerService.class);
        boldPaymentService = new BoldPaymentService(
                reservationService,
                "bold_pk_test_123",
                "bold_sk_test_456",
                "1",
                "USD",
                "http://localhost:4200");
    }

    @Test
    void createsCheckoutSessionWithFixedAmountOfOneAndValidSha256Signature() {
        ReservationDTO dto = ReservationDTO.builder()
                .id(42L)
                .clientId(7L)
                .clientFirstName("Juan")
                .clientLastName("Moreno")
                .clientEmail("juanmorenob1910@gmail.com")
                .status("CONFIRMED")
                .paid(false)
                .totalPrice(850.0)
                .pendingBalance(850.0)
                .build();

        when(reservationService.findSummaryById(42L)).thenReturn(dto);

        BoldCheckoutDTO checkout = boldPaymentService.createCheckoutSession(42L, "/profile/7");

        assertEquals(42L, checkout.getReservationId());
        assertEquals("1", checkout.getAmount());
        assertEquals("USD", checkout.getCurrency());
        assertEquals("bold_pk_test_123", checkout.getApiKey());
        assertTrue(checkout.getOrderId().startsWith("MC-RES-42-"));
        assertEquals("http://localhost:4200/profile/7", checkout.getRedirectionUrl());

        String expectedHash = BoldPaymentService.sha256Hex(
                checkout.getOrderId() + "1" + "USD" + "bold_sk_test_456");
        assertEquals(expectedHash, checkout.getIntegritySignature());
    }

    @Test
    void confirmPaymentMarksReservationAsPaidWhenApproved() {
        ReservationDTO unpaid = ReservationDTO.builder()
                .id(42L)
                .status("CONFIRMED")
                .paid(false)
                .build();
        ReservationDTO paid = ReservationDTO.builder()
                .id(42L)
                .status("CONFIRMED")
                .paid(true)
                .pendingBalance(0.0)
                .build();

        when(reservationService.findSummaryById(42L)).thenReturn(unpaid);
        when(reservationService.payStayAccount(42L)).thenReturn(paid);

        ReservationDTO result = boldPaymentService.confirmPayment(42L, "MC-RES-42-1700000000", "approved");

        assertTrue(result.isPaid());
        assertEquals(0.0, result.getPendingBalance());
        verify(reservationService, times(1)).payStayAccount(42L);
    }
}

