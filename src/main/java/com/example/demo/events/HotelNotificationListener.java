package com.example.demo.events;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.entities.Client;
import com.example.demo.service.EmailService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener asíncrono de eventos de dominio del hotel.
 * Escucha eventos después de que la transacción en base de datos hace commit (AFTER_COMMIT)
 * y dispara los correos automáticos correspondientes a través de EmailService.
 */
@Component
public class HotelNotificationListener {

    public enum ReservationEventType {
        CREATED,
        UPDATED,
        CANCELLED,
        PAID,
        COMPLETED
    }

    public record ClientRegisteredEvent(Client client) {
    }

    public record ReservationLifecycleEvent(ReservationEventType type, ReservationDTO reservation) {
    }

    private final EmailService emailService;

    public HotelNotificationListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async("mailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onClientRegistered(ClientRegisteredEvent event) {
        if (event == null || event.client() == null) {
            return;
        }
        emailService.sendWelcomeEmail(event.client());
    }

    @Async("mailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onReservationLifecycle(ReservationLifecycleEvent event) {
        if (event == null || event.reservation() == null || event.type() == null) {
            return;
        }
        switch (event.type()) {
            case CREATED -> emailService.sendReservationCreatedEmail(event.reservation());
            case UPDATED -> emailService.sendReservationUpdatedEmail(event.reservation());
            case CANCELLED -> emailService.sendReservationCancelledEmail(event.reservation());
            case PAID -> emailService.sendPaymentReceiptEmail(event.reservation());
            case COMPLETED -> emailService.sendStayCompletedEmail(event.reservation());
        }
    }
}

