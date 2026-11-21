package com.example.demo.service;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.events.HotelNotificationListener;
import com.example.demo.events.HotelNotificationListener.ReservationEventType;
import com.example.demo.events.HotelNotificationListener.ReservationLifecycleEvent;
import com.example.demo.service.VoiceCallService.VoiceCallResult;
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

class VoiceCallServiceTest {

    private AiAssistantService aiAssistantService;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        aiAssistantService = mock(AiAssistantService.class);
        when(aiAssistantService.getFormattedHotelContext()).thenReturn(
                "=== INFORMACIÓN EN VIVO DE MONTCLAIR GRAND HOTEL ===\n" +
                "- Horario de Check-in: 15:00 hrs\n" +
                "• Spa & Bienestar: $100.00 | Horario: 8:00 AM - 10:00 PM"
        );
        httpClient = mock(HttpClient.class);
    }

    @Test
    void normalizesPhoneNumbersToE164() {
        VoiceCallServiceImpl service = createBlandService(true, "sk_bland_test");

        assertEquals("+573001234567", service.normalizePhoneNumber("300 123 4567"));
        assertEquals("+34612345678", service.normalizePhoneNumber("+34 612-345-678"));
        assertEquals("+34612345678", service.normalizePhoneNumber("0034 612 345 678"));
        assertNull(service.normalizePhoneNumber("   "));
        assertNull(service.normalizePhoneNumber("123"));
    }

    @Test
    void buildsConversationalPromptWithReservationAndLiveHotelContext() {
        VoiceCallServiceImpl service = createBlandService(true, "sk_bland_test");
        ReservationDTO dto = sampleReservation();

        String firstSentence = service.buildFirstSentence(dto);
        assertTrue(firstSentence.contains("Juan"));
        assertTrue(firstSentence.contains("Sofía"));
        assertTrue(firstSentence.contains("42"));
        assertTrue(firstSentence.contains("Presidential Suite"));

        String prompt = service.buildReservationAgentPrompt(dto);
        assertTrue(prompt.contains("#42"));
        assertTrue(prompt.contains("Juan Pérez"));
        assertTrue(prompt.contains("Presidential Suite"));
        assertTrue(prompt.contains("Spa Privado"));
        assertTrue(prompt.contains("$950.00 USD"));
        assertTrue(prompt.contains("INFORMACIÓN EN VIVO DE MONTCLAIR GRAND HOTEL"));
    }

    @Test
    void doesNotCallWhenVoiceIsDisabled() throws Exception {
        VoiceCallServiceImpl service = createBlandService(false, "sk_bland_test");

        VoiceCallResult result = service.triggerReservationConfirmationCall(sampleReservation());

        assertFalse(result.success());
        verify(httpClient, never()).send(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void dispatchesOutboundCallToBlandAiWhenReservationIsConfirmed() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"status\":\"success\",\"call_id\":\"call_987654\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        VoiceCallServiceImpl service = createBlandService(true, "sk_bland_live_key");
        VoiceCallResult result = service.triggerReservationConfirmationCall(sampleReservation());

        assertTrue(result.success());
        assertEquals("BLAND", result.provider());
        assertEquals("call_987654", result.callId());
        assertEquals("+573001234567", result.toPhone());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(1)).send(captor.capture(), any());
        HttpRequest req = captor.getValue();
        assertEquals("https://api.bland.ai/v1/calls", req.uri().toString());
        assertEquals("sk_bland_live_key", req.headers().firstValue("Authorization").orElse(""));
    }

    @Test
    @SuppressWarnings("unchecked")
    void dispatchesOutboundCallToVapiWhenConfigured() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(201);
        when(mockResponse.body()).thenReturn("{\"id\":\"vapi_call_123\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockResponse);

        VoiceCallServiceImpl service = new VoiceCallServiceImpl(
                true,
                "VAPI",
                "+57",
                "Sofía",
                5,
                "",
                "https://api.bland.ai/v1/calls",
                "Elena",
                "vapi_secret_token",
                "phone_id_99",
                "",
                "https://api.vapi.ai/call",
                "",
                "",
                "",
                "https://api.elevenlabs.io/v1/convai/twilio/outbound-call",
                "",
                aiAssistantService,
                httpClient
        );

        VoiceCallResult result = service.triggerReservationConfirmationCall(sampleReservation());

        assertTrue(result.success());
        assertEquals("VAPI", result.provider());
        assertEquals("vapi_call_123", result.callId());

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(1)).send(captor.capture(), any());
        assertEquals("Bearer vapi_secret_token", captor.getValue().headers().firstValue("Authorization").orElse(""));
    }

    @Test
    void notificationListenerTriggersBothEmailAndVoiceCallOnReservationCreated() {
        EmailService emailService = mock(EmailService.class);
        VoiceCallService voiceCallService = mock(VoiceCallService.class);
        HotelNotificationListener listener = new HotelNotificationListener(emailService, voiceCallService);

        ReservationDTO dto = sampleReservation();
        listener.onReservationLifecycle(new ReservationLifecycleEvent(ReservationEventType.CREATED, dto));

        verify(emailService, times(1)).sendReservationCreatedEmail(dto);
        verify(voiceCallService, times(1)).triggerReservationConfirmationCall(dto);
    }

    private VoiceCallServiceImpl createBlandService(boolean enabled, String blandKey) {
        return new VoiceCallServiceImpl(
                enabled,
                "BLAND",
                "+57",
                "Sofía",
                5,
                blandKey,
                "https://api.bland.ai/v1/calls",
                "Elena",
                "",
                "",
                "",
                "https://api.vapi.ai/call",
                "",
                "",
                "",
                "https://api.elevenlabs.io/v1/convai/twilio/outbound-call",
                "",
                aiAssistantService,
                httpClient
        );
    }

    private ReservationDTO sampleReservation() {
        return ReservationDTO.builder()
                .id(42L)
                .clientId(10L)
                .clientFirstName("Juan")
                .clientLastName("Pérez")
                .clientEmail("juan@example.com")
                .clientPhone("300 123 4567")
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
