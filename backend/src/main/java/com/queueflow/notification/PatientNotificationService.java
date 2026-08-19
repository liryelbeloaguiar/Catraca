package com.queueflow.notification;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class PatientNotificationService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern(
            "dd/MM/yyyy 'às' HH:mm", Locale.forLanguageTag("pt-BR"));
    private final JdbcTemplate jdbc;
    private final MailDeliveryService delivery;
    private final ZoneId zoneId;

    public PatientNotificationService(JdbcTemplate jdbc, MailDeliveryService delivery,
                                      @Value("${app.scheduling.zone-id:America/Sao_Paulo}") String zoneId) {
        this.jdbc = jdbc;
        this.delivery = delivery;
        this.zoneId = ZoneId.of(zoneId);
    }

    public int notifyScheduleChange(UUID scheduleId, String changeDescription) {
        var appointments = jdbc.query("""
                SELECT DISTINCT appointment.id, patient.user_id recipient_user_id,
                       COALESCE(NULLIF(settings.notification_email,''), patient_user.email) email_to,
                       COALESCE(patient_user.full_name, appointment.guest_name, 'Paciente') patient_name,
                       professional_user.full_name professional_name, slot.starts_at,
                       settings.establishment_name
                FROM appointments appointment
                JOIN time_slots slot ON slot.id=appointment.time_slot_id
                JOIN schedules schedule ON schedule.id=slot.schedule_id
                JOIN professionals professional ON professional.id=schedule.professional_id
                JOIN users professional_user ON professional_user.id=professional.user_id
                LEFT JOIN patients patient ON patient.id=appointment.patient_id
                LEFT JOIN users patient_user ON patient_user.id=patient.user_id
                CROSS JOIN organization_settings settings
                WHERE schedule.id=? AND slot.starts_at>=now()
                  AND appointment.status NOT IN ('CANCELLED','COMPLETED','NO_SHOW','RESCHEDULED')
                """, (result, row) -> new AppointmentNotice(
                UUID.fromString(result.getString("id")),
                result.getString("recipient_user_id") == null ? null
                        : UUID.fromString(result.getString("recipient_user_id")),
                result.getString("email_to"), result.getString("patient_name"),
                result.getString("professional_name"), result.getTimestamp("starts_at"),
                result.getString("establishment_name")), scheduleId);
        appointments.forEach(item -> create(item, changeDescription));
        return appointments.size();
    }

    public int notifyProfessionalDisabled(UUID professionalUserId) {
        var scheduleIds = jdbc.queryForList("""
                SELECT id FROM schedules
                WHERE professional_id=(SELECT id FROM professionals WHERE user_id=?)
                  AND valid_until>=current_date
                """, UUID.class, professionalUserId);
        return scheduleIds.stream()
                .mapToInt(id -> notifyScheduleChange(id, "O profissional foi desativado"))
                .sum();
    }

    private void create(AppointmentNotice item, String changeDescription) {
        UUID notificationId = UUID.randomUUID();
        String title = "Alteração no agendamento";
        String date = item.startsAt().toInstant().atZone(zoneId).format(DATE_TIME);
        String message = "Olá, " + item.patientName() + ".\n\n"
                + changeDescription + " e seu agendamento com " + item.professionalName()
                + ", previsto para " + date + ", precisa ser conferido pela equipe.\n\n"
                + "Acompanhe a notificação no sistema ou entre em contato com "
                + item.establishmentName() + ".";
        String status = delivery.enabled() && item.emailTo() != null ? "QUEUED" : "PENDING_CONFIGURATION";
        jdbc.update("""
                INSERT INTO patient_notifications(
                    id,recipient_user_id,appointment_id,notification_type,title,message,email_to,email_status
                ) VALUES (?,?,?,?,?,?,?,?)
                """, notificationId, item.recipientUserId(), item.appointmentId(),
                "SCHEDULE_CHANGE", title, message, item.emailTo(), status);
        if (!"QUEUED".equals(status)) return;
        Runnable send = () -> delivery.send(notificationId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { send.run(); }
            });
        } else {
            send.run();
        }
    }

    private record AppointmentNotice(UUID appointmentId, UUID recipientUserId, String emailTo,
                                     String patientName, String professionalName,
                                     Timestamp startsAt, String establishmentName) {}
}
