package com.queueflow.notification;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final JdbcTemplate jdbc;

    public NotificationController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    List<Map<String,Object>> list(@RequestParam(defaultValue = "50") int size,
                                  @AuthenticationPrincipal Jwt jwt,
                                  Authentication authentication) {
        boolean administrator = hasRole(authentication, "ROLE_ADMIN_USER")
                || hasRole(authentication, "ROLE_DEV_ADMIN");
        return jdbc.queryForList("""
                SELECT notification.id,notification.title,notification.message,
                       notification.email_to,notification.email_status,
                       notification.sent_at,notification.read_at,notification.created_at,
                       appointment.status appointment_status
                FROM patient_notifications notification
                LEFT JOIN appointments appointment ON appointment.id=notification.appointment_id
                WHERE (?=true OR notification.recipient_user_id=?)
                ORDER BY notification.created_at DESC LIMIT ?
                """, administrator, UUID.fromString(jwt.getSubject()), Math.min(Math.max(size, 1), 100));
    }

    @PatchMapping("/{id}/read")
    void read(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        boolean administrator = hasRole(authentication, "ROLE_ADMIN_USER")
                || hasRole(authentication, "ROLE_DEV_ADMIN");
        jdbc.update("UPDATE patient_notifications SET read_at=now() WHERE id=? AND (?=true OR recipient_user_id=?)",
                id, administrator, UUID.fromString(jwt.getSubject()));
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}
