package com.queueflow.audit;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/audit-logs")
public class AuditController {
    private final JdbcTemplate jdbc;
    public AuditController(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @GetMapping @PreAuthorize("hasRole('DEV_ADMIN')")
    List<AuditResponse> list(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return jdbc.query("SELECT a.id,a.action,a.resource_type,a.resource_id,a.details::text,a.occurred_at,a.ip_address::text,u.full_name actor_name,u.email actor_email FROM audit_logs a LEFT JOIN users u ON u.id=a.actor_user_id ORDER BY a.occurred_at DESC LIMIT ? OFFSET ?",
                (rs, row) -> new AuditResponse(UUID.fromString(rs.getString("id")), rs.getString("action"), rs.getString("resource_type"),
                        rs.getString("resource_id"), rs.getString("details"), rs.getTimestamp("occurred_at").toInstant(),
                        rs.getString("ip_address"), rs.getString("actor_name"), rs.getString("actor_email")), safeSize, Math.max(page, 0) * safeSize);
    }
    record AuditResponse(UUID id, String action, String resourceType, String resourceId, String details,
                         Instant occurredAt, String ipAddress, String actorName, String actorEmail) {}
}
