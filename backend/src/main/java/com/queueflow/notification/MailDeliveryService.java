package com.queueflow.notification;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MailDeliveryService {
    private final JdbcTemplate jdbc;
    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String username;

    public MailDeliveryService(JdbcTemplate jdbc, JavaMailSender mailSender,
                               @Value("${app.notifications.mail-enabled:false}") boolean enabled,
                               @Value("${spring.mail.username:}") String username) {
        this.jdbc = jdbc;
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.username = username;
    }

    public boolean enabled() {
        return enabled && !username.isBlank();
    }

    @Async
    public void send(UUID notificationId) {
        if (!enabled()) return;
        var rows = jdbc.query("""
                SELECT notification.email_to, notification.title, notification.message,
                       settings.establishment_name
                FROM patient_notifications notification
                CROSS JOIN organization_settings settings
                WHERE notification.id=? AND notification.email_status='QUEUED'
                """, (result, row) -> new MailData(
                result.getString("email_to"), result.getString("title"),
                result.getString("message"), result.getString("establishment_name")),
                notificationId);
        if (rows.isEmpty()) return;
        MailData data = rows.getFirst();
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(username);
            message.setTo(data.to());
            message.setSubject("[" + data.establishmentName() + "] " + data.title());
            message.setText(data.message());
            mailSender.send(message);
            jdbc.update("UPDATE patient_notifications SET email_status='SENT',sent_at=now(),email_error=NULL WHERE id=?",
                    notificationId);
        } catch (Exception exception) {
            String error = exception.getMessage() == null ? "Falha no envio SMTP" : exception.getMessage();
            jdbc.update("UPDATE patient_notifications SET email_status='FAILED',email_error=? WHERE id=?",
                    error.substring(0, Math.min(error.length(), 500)), notificationId);
        }
    }

    private record MailData(String to, String title, String message, String establishmentName) {}
}
