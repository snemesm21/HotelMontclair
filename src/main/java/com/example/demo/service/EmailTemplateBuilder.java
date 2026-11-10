package com.example.demo.service;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.entities.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Constructor de plantillas HTML con la identidad visual de MontClair Grand Hotel.
 * Diseñado con estilos inline para máxima compatibilidad en clientes de correo (Gmail, Outlook, Apple Mail).
 */
@Component
public class EmailTemplateBuilder {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final String frontendUrl;

    public EmailTemplateBuilder(
            @Value("${hotelmontclair.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    public String buildWelcomeEmail(Client client) {
        String fullName = formatClientName(client != null ? client.getFirstName() : null,
                client != null ? client.getLastName() : null);
        String username = client != null && client.getUsername() != null ? escapeHtml(client.getUsername()) : "-";

        String body = """
                <p style="margin:0 0 16px;font-size:16px;line-height:1.6;color:#334155;">
                    Estimado/a <strong>%s</strong>,
                </p>
                <p style="margin:0 0 16px;font-size:15px;line-height:1.6;color:#475569;">
                    Bienvenido/a a <strong>MontClair Grand Hotel</strong>. Su cuenta de huésped ha sido creada exitosamente.
                    A partir de ahora podrá reservar suites de lujo, personalizar servicios exclusivos y consultar el estado de sus estancias en línea.
                </p>
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="margin:24px 0;background-color:#f8fafc;border:1px solid #e2e8f0;border-left:4px solid #c9a96e;border-radius:6px;">
                    <tr>
                        <td style="padding:16px 20px;">
                            <p style="margin:0 0 6px;font-size:12px;text-transform:uppercase;letter-spacing:1px;color:#64748b;">Usuario registrado</p>
                            <p style="margin:0;font-size:16px;font-weight:600;color:#0f172a;">%s</p>
                        </td>
                    </tr>
                </table>
                %s
                """.formatted(
                fullName,
                username,
                buildCtaButton("Explorar Suites y Reservar", frontendUrl + "/rooms"));

        return wrapLayout("Bienvenido a MontClair Grand Hotel", "Registro Confirmado", body);
    }

    public String buildReservationCreatedEmail(ReservationDTO dto) {
        String fullName = formatClientName(dto.getClientFirstName(), dto.getClientLastName());
        String body = """
                <p style="margin:0 0 16px;font-size:16px;line-height:1.6;color:#334155;">
                    Estimado/a <strong>%s</strong>,
                </p>
                <p style="margin:0 0 20px;font-size:15px;line-height:1.6;color:#475569;">
                    Nos complace confirmar su reserva <strong>#%d</strong> en <strong>MontClair Grand Hotel</strong>.
                    Nuestro equipo ya está preparando cada detalle para garantizarle una estancia inolvidable.
                </p>
                %s
                %s
                """.formatted(
                fullName,
                dto.getId(),
                buildReservationSummaryTable(dto, "#166534", "#dcfce7", "CONFIRMADA"),
                buildCtaButton("Ver Mi Reserva", frontendUrl + "/profile/" + dto.getClientId()));

        return wrapLayout("Reserva Confirmada #" + dto.getId(), "Confirmación de Reserva", body);
    }

    public String buildReservationUpdatedEmail(ReservationDTO dto) {
        String fullName = formatClientName(dto.getClientFirstName(), dto.getClientLastName());
        String body = """
                <p style="margin:0 0 16px;font-size:16px;line-height:1.6;color:#334155;">
                    Estimado/a <strong>%s</strong>,
                </p>
                <p style="margin:0 0 20px;font-size:15px;line-height:1.6;color:#475569;">
                    Le informamos que los detalles de su reserva <strong>#%d</strong> han sido actualizados correctamente.
                    A continuación encontrará el resumen vigente de su estancia:
                </p>
                %s
                %s
                """.formatted(
                fullName,
                dto.getId(),
                buildReservationSummaryTable(dto, "#1e40af", "#dbeafe", "ACTUALIZADA"),
                buildCtaButton("Consultar Detalles", frontendUrl + "/profile/" + dto.getClientId()));

        return wrapLayout("Reserva Actualizada #" + dto.getId(), "Modificación de Reserva", body);
    }

    public String buildReservationCancelledEmail(ReservationDTO dto) {
        String fullName = formatClientName(dto.getClientFirstName(), dto.getClientLastName());
        String body = """
                <p style="margin:0 0 16px;font-size:16px;line-height:1.6;color:#334155;">
                    Estimado/a <strong>%s</strong>,
                </p>
                <p style="margin:0 0 20px;font-size:15px;line-height:1.6;color:#475569;">
                    Confirmamos que su reserva <strong>#%d</strong> ha sido cancelada conforme a su solicitud.
                    Esperamos tener el privilegio de recibirle en una próxima ocasión.
                </p>
                %s
                %s
                """.formatted(
                fullName,
                dto.getId(),
                buildReservationSummaryTable(dto, "#991b1b", "#fee2e2", "CANCELADA"),
                buildCtaButton("Realizar Nueva Reserva", frontendUrl + "/rooms"));

        return wrapLayout("Reserva Cancelada #" + dto.getId(), "Cancelación de Reserva", body);
    }

    public String buildPaymentReceiptEmail(ReservationDTO dto) {
        String fullName = formatClientName(dto.getClientFirstName(), dto.getClientLastName());
        String body = """
                <p style="margin:0 0 16px;font-size:16px;line-height:1.6;color:#334155;">
                    Estimado/a <strong>%s</strong>,
                </p>
                <p style="margin:0 0 20px;font-size:15px;line-height:1.6;color:#475569;">
                    Hemos registrado exitosamente el pago total de la cuenta correspondiente a su reserva <strong>#%d</strong>.
                    Su saldo pendiente actual es de <strong>$0.00</strong>.
                </p>
                %s
                %s
                """.formatted(
                fullName,
                dto.getId(),
                buildReservationSummaryTable(dto, "#065f46", "#d1fae5", "CUENTA PAGADA"),
                buildCtaButton("Ver Historial de Estancias", frontendUrl + "/profile/" + dto.getClientId()));

        return wrapLayout("Comprobante de Pago - Reserva #" + dto.getId(), "Recibo de Pago", body);
    }

    public String buildStayCompletedEmail(ReservationDTO dto) {
        String fullName = formatClientName(dto.getClientFirstName(), dto.getClientLastName());
        String body = """
                <p style="margin:0 0 16px;font-size:16px;line-height:1.6;color:#334155;">
                    Estimado/a <strong>%s</strong>,
                </p>
                <p style="margin:0 0 20px;font-size:15px;line-height:1.6;color:#475569;">
                    Ha sido un honor contar con su presencia en <strong>MontClair Grand Hotel</strong>.
                    Su proceso de Check-out para la reserva <strong>#%d</strong> ha finalizado satisfactoriamente.
                </p>
                %s
                %s
                """.formatted(
                fullName,
                dto.getId(),
                buildReservationSummaryTable(dto, "#334155", "#e2e8f0", "ESTANCIA FINALIZADA"),
                buildCtaButton("Reservar Próxima Estancia", frontendUrl + "/rooms"));

        return wrapLayout("Gracias por su visita - Reserva #" + dto.getId(), "Check-Out Completado", body);
    }

    private String buildReservationSummaryTable(
            ReservationDTO dto,
            String badgeTextColor,
            String badgeBgColor,
            String badgeLabel) {

        String checkIn = dto.getCheckInDate() != null ? dto.getCheckInDate().format(DATE_FORMATTER) : "-";
        String checkOut = dto.getCheckOutDate() != null ? dto.getCheckOutDate().format(DATE_FORMATTER) : "-";
        String roomLabel = (dto.getRoomTypeName() != null && !dto.getRoomTypeName().isBlank()
                ? escapeHtml(dto.getRoomTypeName())
                : "Suite")
                + (dto.getRoomNumber() != null && !dto.getRoomNumber().isBlank()
                        ? " — Habitación #" + escapeHtml(dto.getRoomNumber())
                        : "");

        StringBuilder servicesRows = new StringBuilder();
        List<ReservationDTO.ServiceDetail> services = dto.getServices();
        if (services != null && !services.isEmpty()) {
            for (ReservationDTO.ServiceDetail service : services) {
                servicesRows.append("""
                        <tr>
                            <td style="padding:8px 16px;border-bottom:1px solid #e2e8f0;font-size:13px;color:#475569;">
                                Servicio: %s (x%d)
                            </td>
                            <td align="right" style="padding:8px 16px;border-bottom:1px solid #e2e8f0;font-size:13px;color:#0f172a;font-weight:500;">
                                %s
                            </td>
                        </tr>
                        """.formatted(
                        escapeHtml(service.getName()),
                        service.getQuantity(),
                        formatCurrency(service.getSubtotal())));
            }
        }

        return """
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="margin:20px 0;border:1px solid #e2e8f0;border-radius:8px;overflow:hidden;">
                    <tr style="background-color:#0f172a;color:#ffffff;">
                        <td style="padding:14px 16px;font-size:14px;font-weight:600;letter-spacing:0.5px;">
                            Reserva #%d
                        </td>
                        <td align="right" style="padding:14px 16px;">
                            <span style="display:inline-block;padding:4px 10px;border-radius:999px;font-size:11px;font-weight:700;letter-spacing:0.5px;color:%s;background-color:%s;">
                                %s
                            </span>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding:10px 16px;border-bottom:1px solid #e2e8f0;font-size:13px;color:#64748b;">Habitación asignada</td>
                        <td align="right" style="padding:10px 16px;border-bottom:1px solid #e2e8f0;font-size:13px;color:#0f172a;font-weight:600;">%s</td>
                    </tr>
                    <tr>
                        <td style="padding:10px 16px;border-bottom:1px solid #e2e8f0;font-size:13px;color:#64748b;">Check-In / Check-Out</td>
                        <td align="right" style="padding:10px 16px;border-bottom:1px solid #e2e8f0;font-size:13px;color:#0f172a;">%s → %s (%d noche%s)</td>
                    </tr>
                    <tr>
                        <td style="padding:10px 16px;border-bottom:1px solid #e2e8f0;font-size:13px;color:#64748b;">Huéspedes</td>
                        <td align="right" style="padding:10px 16px;border-bottom:1px solid #e2e8f0;font-size:13px;color:#0f172a;">%d persona%s</td>
                    </tr>
                    <tr>
                        <td style="padding:10px 16px;border-bottom:1px solid #e2e8f0;font-size:13px;color:#475569;">Alojamiento (%d × %s)</td>
                        <td align="right" style="padding:10px 16px;border-bottom:1px solid #e2e8f0;font-size:13px;color:#0f172a;font-weight:500;">%s</td>
                    </tr>
                    %s
                    <tr style="background-color:#f8fafc;">
                        <td style="padding:12px 16px;border-bottom:1px solid #e2e8f0;font-size:14px;font-weight:700;color:#0f172a;">Total de la Estancia</td>
                        <td align="right" style="padding:12px 16px;border-bottom:1px solid #e2e8f0;font-size:15px;font-weight:700;color:#0f172a;">%s</td>
                    </tr>
                    <tr style="background-color:#f8fafc;">
                        <td style="padding:10px 16px;font-size:13px;color:#64748b;">Monto Abonado / Saldo Pendiente</td>
                        <td align="right" style="padding:10px 16px;font-size:13px;color:#0f172a;">
                            Abonado: <strong>%s</strong> | Pendiente: <strong>%s</strong>
                        </td>
                    </tr>
                </table>
                """.formatted(
                dto.getId(),
                badgeTextColor,
                badgeBgColor,
                badgeLabel,
                roomLabel,
                checkIn,
                checkOut,
                dto.getNights(),
                dto.getNights() == 1 ? "" : "s",
                dto.getNumberOfPeople(),
                dto.getNumberOfPeople() == 1 ? "" : "s",
                dto.getNights(),
                formatCurrency(dto.getPricePerNight()),
                formatCurrency(dto.getRoomSubtotal()),
                servicesRows.toString(),
                formatCurrency(dto.getTotalPrice()),
                formatCurrency(dto.getPaidAmount()),
                formatCurrency(dto.getPendingBalance()));
    }

    private String buildCtaButton(String label, String url) {
        return """
                <div style="text-align:center;margin:28px 0 8px;">
                    <a href="%s" style="display:inline-block;padding:12px 28px;background-color:#c9a96e;color:#0f172a;text-decoration:none;font-weight:700;font-size:14px;border-radius:6px;letter-spacing:0.5px;">
                        %s
                    </a>
                </div>
                """.formatted(escapeHtml(url), escapeHtml(label));
    }

    private String wrapLayout(String title, String subtitle, String innerHtml) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f1f5f9;font-family:'Segoe UI',Helvetica,Arial,sans-serif;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#f1f5f9;padding:32px 16px;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="max-width:600px;width:100%%;background-color:#ffffff;border-radius:10px;overflow:hidden;box-shadow:0 4px 12px rgba(15,23,42,0.08);">
                                    <tr>
                                        <td style="background-color:#0f172a;padding:28px 32px;text-align:center;border-bottom:3px solid #c9a96e;">
                                            <p style="margin:0 0 6px;font-size:11px;letter-spacing:3px;text-transform:uppercase;color:#c9a96e;">%s</p>
                                            <h1 style="margin:0;font-family:Georgia,serif;font-size:24px;font-weight:400;letter-spacing:1.5px;color:#ffffff;">
                                                MONTCLAIR GRAND HOTEL
                                            </h1>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:32px;">
                                            %s
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="background-color:#f8fafc;padding:20px 32px;text-align:center;border-top:1px solid #e2e8f0;">
                                            <p style="margin:0 0 4px;font-size:12px;color:#64748b;">
                                                MontClair Grand Hotel &bull; Concierge &amp; Reservations
                                            </p>
                                            <p style="margin:0;font-size:11px;color:#94a3b8;">
                                                Este es un mensaje automático generado por el sistema de reservas.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(escapeHtml(title), escapeHtml(subtitle), innerHtml);
    }

    private String formatClientName(String firstName, String lastName) {
        String first = firstName != null ? firstName.trim() : "";
        String last = lastName != null ? lastName.trim() : "";
        String combined = (first + " " + last).trim();
        return combined.isEmpty() ? "Huésped" : escapeHtml(combined);
    }

    private String formatCurrency(double amount) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.US);
        return nf.format(amount);
    }

    public static String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

