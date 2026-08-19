package com.queueflow.configuration;

import com.queueflow.audit.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/settings/organization")
@PreAuthorize("hasAnyRole('DEV_ADMIN', 'ADMIN_USER')")
public class OrganizationSettingsController {
    private final JdbcTemplate jdbc;
    private final AuditService audit;

    public OrganizationSettingsController(JdbcTemplate jdbc, AuditService audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    @GetMapping
    OrganizationSettingsResponse get() {
        return jdbc.queryForObject(
                "SELECT establishment_name, notification_email, updated_at FROM organization_settings WHERE id=1",
                (result, row) -> new OrganizationSettingsResponse(
                        result.getString("establishment_name"),
                        result.getString("notification_email"),
                        result.getTimestamp("updated_at").toInstant()));
    }

    @PatchMapping
    @Transactional
    OrganizationSettingsResponse update(
            @Valid @RequestBody UpdateOrganizationSettings request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = UUID.fromString(jwt.getSubject());
        String establishmentName = request.establishmentName().trim();
        String notificationEmail = request.notificationEmail().trim().toLowerCase();
        jdbc.update("""
                UPDATE organization_settings
                SET establishment_name=?, notification_email=?, updated_by=?, updated_at=now()
                WHERE id=1
                """, establishmentName, notificationEmail, actorId);
        audit.record(actorId, "ORGANIZATION_SETTINGS_UPDATE", "organization_settings", null,
                Map.of("establishmentName", establishmentName, "notificationEmail", notificationEmail));
        return get();
    }

    public record UpdateOrganizationSettings(
            @NotBlank @Size(max = 160) String establishmentName,
            @Email @NotBlank @Size(max = 254) String notificationEmail) {}

    public record OrganizationSettingsResponse(
            String establishmentName, String notificationEmail, java.time.Instant updatedAt) {}
}
