package com.example.demo.service;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.entities.Client;
import com.example.demo.events.HotelNotificationListener;
import com.example.demo.events.HotelNotificationListener.ClientRegisteredEvent;
import com.example.demo.events.HotelNotificationListener.ReservationEventType;
import com.example.demo.events.HotelNotificationListener.ReservationLifecycleEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResendEmailServiceTest {

    private EmailTemplateBuilder templateBuilder;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        templateBuilder = new EmailTemplateBuilder("http://localhost:4200");
        httpClient = mock(HttpClient.class);
    }

    @Test
    void doesNotSendWhenDisabled() throws Exception {
        ResendEmailServiceImpl service = new ResendEmailServiceImpl(
                false,
                "re_test_123",
                "MontClair Grand Hotel <onboarding@resend.dev>",
                "concierge@montclairhotel.com",
                "https://api.resend.com/emails",
                templateBuilder,
                httpClient);

        boolean sent = service.sendHtmlEmail("guest@example.com", "Asunto", "<p>Hola</p>");

        assertFalse(sent);
        verify(httpClient, never()).send(any(), any());
    }

    @Test
    void doesNotSendWhenApiKeyIsBlank() throws Exception {
        ResendEmailServiceImpl service = new ResendEmailServiceImpl(
                true,
                "   ",
                "MontClair Grand Hotel <onboarding@resend.dev>",
                "concierge@montclairhotel.com",
                "https://api.resend.com/emails",
                templateBuilder,
                httpClient);

        boolean sent = service.sendHtmlEmail("guest@example.com", "Asunto", "<p>Hola</p>");

        assertFalse(sent);
        verify(httpClient, never()).send(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendsPostRequestToResendApiWithBearerToken() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"id\":\"email_abc123\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        ResendEmailServiceImpl service = new ResendEmailServiceImpl(
                true,
                "re_live_secret_key",
                "MontClair Grand Hotel <onboarding@resend.dev>",
                "concierge@montclairhotel.com",
                "https://api.resend.com/emails",
                templateBuilder,
                httpClient);

        ReservationDTO dto = sampleReservation();
        service.sendReservationCreatedEmail(dto);

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(1)).send(requestCaptor.capture(), any());

        HttpRequest captured = requestCaptor.getValue();
        assertEquals("https://api.resend.com/emails", captured.uri().toString());
        assertEquals("Bearer re_live_secret_key", captured.headers().firstValue("Authorization").orElse(""));
        assertEquals("application/json", captured.headers().firstValue("Content-Type").orElse(""));
    }

    @Test
    void templatesIncludeHotelBrandingAndReservationData() {
        Client client = Client.builder()
                .id(10L)
                .username("juan.perez")
                .firstName("Juan")
                .lastName("Pérez")
                .email("juan@example.com")
                .build();

        String welcomeHtml = templateBuilder.buildWelcomeEmail(client);
        assertTrue(welcomeHtml.contains("MONTCLAIR GRAND HOTEL"));
        assertTrue(welcomeHtml.contains("Juan Pérez"));
        assertTrue(welcomeHtml.contains("juan.perez"));

        ReservationDTO dto = sampleReservation();
        String createdHtml = templateBuilder.buildReservationCreatedEmail(dto);
        assertTrue(createdHtml.contains("Reserva #42"));
        assertTrue(createdHtml.contains("Presidential Suite"));
        assertTrue(createdHtml.contains("Spa Privado"));
        assertTrue(createdHtml.contains("$950.00"));
    }

    @Test
    void listenerDelegatesEventsToEmailService() {
        EmailService emailService = mock(EmailService.class);
        HotelNotificationListener listener = new HotelNotificationListener(emailService);

        Client client = Client.builder().id(1L).email("test@example.com").firstName("Ana").build();
        listener.onClientRegistered(new ClientRegisteredEvent(client));
        verify(emailService, times(1)).sendWelcomeEmail(client);

        ReservationDTO dto = sampleReservation();
        listener.onReservationLifecycle(new ReservationLifecycleEvent(ReservationEventType.CREATED, dto));
        listener.onReservationLifecycle(new ReservationLifecycleEvent(ReservationEventType.UPDATED, dto));
        listener.onReservationLifecycle(new ReservationLifecycleEvent(ReservationEventType.CANCELLED, dto));
        listener.onReservationLifecycle(new ReservationLifecycleEvent(ReservationEventType.PAID, dto));
        listener.onReservationLifecycle(new ReservationLifecycleEvent(ReservationEventType.COMPLETED, dto));

        verify(emailService, times(1)).sendReservationCreatedEmail(dto);
        verify(emailService, times(1)).sendReservationUpdatedEmail(dto);
        verify(emailService, times(1)).sendReservationCancelledEmail(dto);
        verify(emailService, times(1)).sendPaymentReceiptEmail(dto);
        verify(emailService, times(1)).sendStayCompletedEmail(dto);
    }

    private ReservationDTO sampleReservation() {
        return ReservationDTO.builder()
                .id(42L)
                .clientId(10L)
                .clientFirstName("Juan")
                .clientLastName("Pérez")
                .clientEmail("juan@example.com")
                .roomNumber("501")
                .roomName("MontClair Royal")
                .roomTypeName("Presidential Suite")
                .pricePerNight(400.0)
                .checkInDate(LocalDate.of(2026, 12, 10))
                .checkOutDate(LocalDate.of(2026, 12, 12))
                .nights(2)
                .numberOfPeople(2)
                .roomSubtotal(800.0)
                .servicesSubtotal(150.0)
                .totalPrice(950.0)
                .paidAmount(0.0)
                .pendingBalance(950.0)
                .status("CONFIRMED")
                .services(List.of(
                        ReservationDTO.ServiceDetail.builder()
                                .id(1L)
                                .name("Spa Privado")
                                .quantity(1)
                                .unitPrice(150.0)
                                .subtotal(150.0)
                                .build()))
                .build();
    }
}

